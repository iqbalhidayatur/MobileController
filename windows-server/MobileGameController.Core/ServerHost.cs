using System.Net.Sockets;

namespace MobileGameController.Core;

/// <summary>
/// High-level host that wires ViGEm virtual controller and TCP server together.
/// Used by both the console and desktop applications.
/// </summary>
public sealed class ServerHost : IDisposable
{
    private VirtualControllerManager? _controllerManager;
    private ControllerTcpServer? _server;
    private CancellationTokenSource? _runCts;
    private Task? _runTask;
    private readonly object _gate = new();

    public event Action<string>? LogMessage;
    public event Action<ServerStatus>? StatusChanged;

    public ServerStatus Status { get; private set; } = new();
    public int Port { get; private set; }

    public bool IsRunning => Status.State is ServerRunState.Listening or ServerRunState.ClientConnected;

    public async Task StartAsync(int port, CancellationToken cancellationToken = default)
    {
        lock (_gate)
        {
            if (IsRunning)
            {
                throw new InvalidOperationException("Server is already running.");
            }

            Port = port;
            UpdateStatus(ServerRunState.Starting, port);
        }

        _runCts = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);

        try
        {
            _controllerManager = new VirtualControllerManager();
            _server = new ControllerTcpServer(_controllerManager);
            _server.LogMessage += ForwardLog;
            _server.ClientConnected += OnClientConnected;
            _server.ClientDisconnected += OnClientDisconnected;

            UpdateStatus(ServerRunState.Listening, port, isViGEmReady: true);
            Log("Virtual Xbox 360 controller ready.");

            _runTask = _server.StartAsync(port, _runCts.Token);
            await _runTask;
        }
        catch (Exception ex) when (IsViGEmError(ex))
        {
            UpdateStatus(ServerRunState.Error, port, errorMessage:
                "ViGEmBus driver not found. Install from github.com/ViGEm/ViGEmBus/releases");
            Log("ViGEmBus driver not found.");
            throw;
        }
        catch (SocketException ex)
        {
            UpdateStatus(ServerRunState.Error, port, errorMessage: $"Cannot bind to port {port}: {ex.Message}");
            Log($"Cannot bind to port {port}: {ex.Message}");
            throw;
        }
        catch (OperationCanceledException)
        {
            // Expected during StopAsync
        }
        finally
        {
            CleanupResources();
            UpdateStatus(ServerRunState.Stopped, port);
            Log("Server stopped.");
        }
    }

    public Task StartBackgroundAsync(int port, CancellationToken cancellationToken = default)
    {
        return Task.Run(async () =>
        {
            try
            {
                await StartAsync(port, cancellationToken);
            }
            catch (SocketException)
            {
                // Status already reported via StatusChanged
            }
            catch (Exception ex) when (IsViGEmError(ex))
            {
                // Status already reported via StatusChanged
            }
        }, CancellationToken.None);
    }

    public async Task StopAsync()
    {
        _runCts?.Cancel();
        _server?.Stop();

        if (_runTask != null)
        {
            try
            {
                await _runTask;
            }
            catch (OperationCanceledException)
            {
                // Expected
            }
        }

        CleanupResources();
        UpdateStatus(ServerRunState.Stopped, Port);
    }

    private void OnClientConnected(string endpoint)
    {
        UpdateStatus(ServerRunState.ClientConnected, Port, connectedClient: endpoint, isViGEmReady: true);
    }

    private void OnClientDisconnected()
    {
        UpdateStatus(ServerRunState.Listening, Port, isViGEmReady: true);
    }

    private void ForwardLog(string message) => Log(message);

    private void Log(string message) => LogMessage?.Invoke(message);

    private void UpdateStatus(
        ServerRunState state,
        int port,
        string? connectedClient = null,
        string? errorMessage = null,
        bool isViGEmReady = false)
    {
        Status = new ServerStatus
        {
            State = state,
            Port = port,
            ConnectedClient = connectedClient,
            ErrorMessage = errorMessage,
            IsViGEmReady = isViGEmReady
        };
        StatusChanged?.Invoke(Status);
    }

    private void CleanupResources()
    {
        if (_server != null)
        {
            _server.LogMessage -= ForwardLog;
            _server.ClientConnected -= OnClientConnected;
            _server.ClientDisconnected -= OnClientDisconnected;
            _server.Dispose();
            _server = null;
        }

        _controllerManager?.Dispose();
        _controllerManager = null;

        _runCts?.Dispose();
        _runCts = null;
        _runTask = null;
    }

    private static bool IsViGEmError(Exception ex) =>
        ex.Message.Contains("ViGEm", StringComparison.OrdinalIgnoreCase) ||
        ex.GetType().Name.Contains("ViGEm", StringComparison.OrdinalIgnoreCase);

    public void Dispose()
    {
        StopAsync().GetAwaiter().GetResult();
    }
}
