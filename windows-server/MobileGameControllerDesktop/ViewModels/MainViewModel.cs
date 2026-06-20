using System.Collections.ObjectModel;
using System.ComponentModel;
using System.Runtime.CompilerServices;
using System.Windows;
using System.Windows.Input;
using MobileGameController.Core;

namespace MobileGameControllerDesktop.ViewModels;

public sealed class MainViewModel : INotifyPropertyChanged, IDisposable
{
    private const int DefaultPort = 27015;

    private readonly ServerHost _host = new();
    private string _portText = DefaultPort.ToString();
    private string _statusText = "Stopped";
    private string _statusDetail = "Start the server, then connect from your phone.";
    private string _connectedClient = "—";
    private string _primaryButtonText = "Start Server";
    private bool _isPortEnabled = true;
    private bool _canCopyAddress;
    private string _selectedAddress = string.Empty;

    public MainViewModel()
    {
        LocalAddresses = new ObservableCollection<string>(NetworkHelper.GetLocalIPv4Addresses());
        LogEntries = new ObservableCollection<string>();

        if (LocalAddresses.Count > 0)
        {
            SelectedAddress = LocalAddresses[0];
        }

        ToggleServerCommand = new RelayCommand(async () => await ToggleServerAsync(), () => !IsBusy);
        CopyAddressCommand = new RelayCommand(CopyAddress, () => CanCopyAddress);
        RefreshAddressesCommand = new RelayCommand(RefreshAddresses);

        _host.LogMessage += OnLogMessage;
        _host.StatusChanged += OnStatusChanged;

        AppendLog("Ready. Install ViGEmBus if you have not already.");
    }

    public ObservableCollection<string> LocalAddresses { get; }
    public ObservableCollection<string> LogEntries { get; }

    public string PortText
    {
        get => _portText;
        set
        {
            if (_portText == value) return;
            _portText = value;
            OnPropertyChanged();
        }
    }

    public string SelectedAddress
    {
        get => _selectedAddress;
        set
        {
            if (_selectedAddress == value) return;
            _selectedAddress = value;
            OnPropertyChanged();
            OnPropertyChanged(nameof(FullConnectionAddress));
            CanCopyAddress = _host.IsRunning && !string.IsNullOrWhiteSpace(_selectedAddress);
        }
    }

    public string FullConnectionAddress =>
        string.IsNullOrWhiteSpace(SelectedAddress) ? "—" : $"{SelectedAddress}:{EffectivePort}";

    public string StatusText
    {
        get => _statusText;
        private set
        {
            if (_statusText == value) return;
            _statusText = value;
            OnPropertyChanged();
        }
    }

    public string StatusDetail
    {
        get => _statusDetail;
        private set
        {
            if (_statusDetail == value) return;
            _statusDetail = value;
            OnPropertyChanged();
        }
    }

    public string ConnectedClient
    {
        get => _connectedClient;
        private set
        {
            if (_connectedClient == value) return;
            _connectedClient = value;
            OnPropertyChanged();
        }
    }

    public string PrimaryButtonText
    {
        get => _primaryButtonText;
        private set
        {
            if (_primaryButtonText == value) return;
            _primaryButtonText = value;
            OnPropertyChanged();
        }
    }

    public bool IsPortEnabled
    {
        get => _isPortEnabled;
        private set
        {
            if (_isPortEnabled == value) return;
            _isPortEnabled = value;
            OnPropertyChanged();
        }
    }

    public bool CanCopyAddress
    {
        get => _canCopyAddress;
        private set
        {
            if (_canCopyAddress == value) return;
            _canCopyAddress = value;
            OnPropertyChanged();
            ((RelayCommand)CopyAddressCommand).RaiseCanExecuteChanged();
        }
    }

    public bool IsBusy { get; private set; }

    public ICommand ToggleServerCommand { get; }
    public ICommand CopyAddressCommand { get; }
    public ICommand RefreshAddressesCommand { get; }

    public int EffectivePort =>
        int.TryParse(PortText, out var port) && port is >= 1 and <= 65535
            ? port
            : DefaultPort;

    private async Task ToggleServerAsync()
    {
        if (_host.IsRunning)
        {
            await StopServerAsync();
            return;
        }

        if (!int.TryParse(PortText, out var port) || port is < 1 or > 65535)
        {
            AppendLog("Invalid port. Use a value between 1 and 65535.");
            StatusText = "Error";
            StatusDetail = "Invalid port number.";
            return;
        }

        SetBusy(true);
        AppendLog($"Starting server on port {port}...");

        try
        {
            _ = _host.StartBackgroundAsync(port);
            await Task.Delay(300);
        }
        catch (Exception ex)
        {
            AppendLog(ex.Message);
            SetBusy(false);
        }
    }

    private async Task StopServerAsync()
    {
        SetBusy(true);
        AppendLog("Stopping server...");
        await _host.StopAsync();
        SetBusy(false);
    }

    private void OnStatusChanged(ServerStatus status)
    {
        Application.Current.Dispatcher.Invoke(() =>
        {
            StatusText = status.State switch
            {
                ServerRunState.Stopped => "Stopped",
                ServerRunState.Starting => "Starting",
                ServerRunState.Listening => "Waiting for phone",
                ServerRunState.ClientConnected => "Connected",
                ServerRunState.Error => "Error",
                _ => status.State.ToString()
            };

            StatusDetail = status.State switch
            {
                ServerRunState.Stopped => "Start the server, then connect from your phone.",
                ServerRunState.Starting => "Initializing virtual Xbox 360 controller...",
                ServerRunState.Listening =>
                    "Open the Android app, enter the address below, and tap Connect.",
                ServerRunState.ClientConnected =>
                    "Controller input is being forwarded to Windows games.",
                ServerRunState.Error => status.ErrorMessage ?? "Unknown error.",
                _ => string.Empty
            };

            ConnectedClient = status.ConnectedClient ?? "—";
            PrimaryButtonText = status.State is ServerRunState.Stopped or ServerRunState.Error
                ? "Start Server"
                : "Stop Server";
            IsPortEnabled = !_host.IsRunning;
            CanCopyAddress = _host.IsRunning && !string.IsNullOrWhiteSpace(SelectedAddress);
            OnPropertyChanged(nameof(FullConnectionAddress));

            if (status.State is ServerRunState.Stopped or ServerRunState.Error)
            {
                SetBusy(false);
            }
            else if (status.State is ServerRunState.Listening or ServerRunState.ClientConnected)
            {
                SetBusy(false);
            }
        });
    }

    private void OnLogMessage(string message)
    {
        Application.Current.Dispatcher.Invoke(() => AppendLog(message));
    }

    private void AppendLog(string message)
    {
        var entry = $"[{DateTime.Now:HH:mm:ss}] {message}";
        LogEntries.Insert(0, entry);
        while (LogEntries.Count > 200)
        {
            LogEntries.RemoveAt(LogEntries.Count - 1);
        }
    }

    private void CopyAddress()
    {
        if (string.IsNullOrWhiteSpace(FullConnectionAddress) || FullConnectionAddress == "—")
        {
            return;
        }

        Clipboard.SetText(FullConnectionAddress);
        AppendLog($"Copied {FullConnectionAddress} to clipboard.");
    }

    private void RefreshAddresses()
    {
        LocalAddresses.Clear();
        foreach (var address in NetworkHelper.GetLocalIPv4Addresses())
        {
            LocalAddresses.Add(address);
        }

        if (LocalAddresses.Count == 0)
        {
            SelectedAddress = string.Empty;
            AppendLog("No local IPv4 address found.");
            return;
        }

        if (!LocalAddresses.Contains(SelectedAddress))
        {
            SelectedAddress = LocalAddresses[0];
        }

        AppendLog("Refreshed local IP addresses.");
    }

    private void SetBusy(bool busy)
    {
        IsBusy = busy;
        ((RelayCommand)ToggleServerCommand).RaiseCanExecuteChanged();
    }

    public void Dispose()
    {
        _host.LogMessage -= OnLogMessage;
        _host.StatusChanged -= OnStatusChanged;
        _host.Dispose();
    }

    public event PropertyChangedEventHandler? PropertyChanged;

    private void OnPropertyChanged([CallerMemberName] string? propertyName = null) =>
        PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(propertyName));
}
