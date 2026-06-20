using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Text.Json;
using MobileGameController.Core.Models;

namespace MobileGameController.Core;

/// <summary>
/// TCP server that receives JSON controller events from Android
/// and forwards them to the virtual Xbox 360 controller.
/// </summary>
public class ControllerTcpServer : IDisposable
{
    private readonly VirtualControllerManager _controllerManager;
    private TcpListener? _listener;
    private CancellationTokenSource? _cts;
    private bool _disposed;

    private static readonly JsonSerializerOptions JsonOptions = new()
    {
        PropertyNameCaseInsensitive = true
    };

    public event Action<string>? LogMessage;
    public event Action<string>? ClientConnected;
    public event Action? ClientDisconnected;

    public bool IsClientConnected { get; private set; }
    public string? ConnectedClientEndpoint { get; private set; }

    public ControllerTcpServer(VirtualControllerManager controllerManager)
    {
        _controllerManager = controllerManager;
    }

    public async Task StartAsync(int port, CancellationToken cancellationToken = default)
    {
        _cts = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
        _listener = new TcpListener(IPAddress.Any, port);
        _listener.Start();

        Log($"Listening on port {port}...");
        Log("Waiting for Android client connection...");

        while (!_cts.Token.IsCancellationRequested)
        {
            try
            {
                var client = await _listener.AcceptTcpClientAsync(_cts.Token);
                client.NoDelay = true;
                client.ReceiveBufferSize = 4096;
                client.SendBufferSize = 4096;

                var endpoint = client.Client.RemoteEndPoint?.ToString() ?? "unknown";
                IsClientConnected = true;
                ConnectedClientEndpoint = endpoint;
                Log($"Client connected: {endpoint}");
                ClientConnected?.Invoke(endpoint);

                _ = HandleClientAsync(client, _cts.Token);
            }
            catch (OperationCanceledException)
            {
                break;
            }
            catch (Exception ex)
            {
                Log($"Accept error: {ex.Message}");
            }
        }
    }

    private async Task HandleClientAsync(TcpClient client, CancellationToken cancellationToken)
    {
        using (client)
        {
            var stream = client.GetStream();
            using var reader = new StreamReader(stream, Encoding.UTF8);
            using var writer = new StreamWriter(stream, Encoding.UTF8) { AutoFlush = true };

            try
            {
                while (!cancellationToken.IsCancellationRequested && client.Connected)
                {
                    var line = await reader.ReadLineAsync(cancellationToken);
                    if (line == null) break;

                    ProcessMessage(line, writer);
                }
            }
            catch (OperationCanceledException)
            {
                // Server shutting down
            }
            catch (Exception ex)
            {
                Log($"Client error: {ex.Message}");
            }
            finally
            {
                _controllerManager.ResetController();
                IsClientConnected = false;
                ConnectedClientEndpoint = null;
                Log("Client disconnected. Controller reset.");
                ClientDisconnected?.Invoke();
            }
        }
    }

    private void ProcessMessage(string json, StreamWriter writer)
    {
        try
        {
            var message = JsonSerializer.Deserialize<ControllerMessage>(json, JsonOptions);
            if (message == null) return;

            switch (message.Type.ToLowerInvariant())
            {
                case "button":
                    if (message.Button != null)
                    {
                        if (message.Button.Equals("LT", StringComparison.OrdinalIgnoreCase) ||
                            message.Button.Equals("RT", StringComparison.OrdinalIgnoreCase))
                        {
                            _controllerManager.HandleTrigger(message.Button, message.Pressed);
                        }
                        else
                        {
                            _controllerManager.HandleButton(message.Button, message.Pressed);
                        }
                    }
                    break;

                case "analog":
                    if (message.Stick != null)
                    {
                        _controllerManager.HandleAnalog(message.Stick, message.X, message.Y);
                    }
                    break;

                case "ping":
                    var pong = $"{{\"type\":\"pong\",\"timestamp\":{message.Timestamp}}}\n";
                    writer.Write(pong);
                    break;
            }
        }
        catch (JsonException ex)
        {
            Log($"Invalid JSON: {ex.Message}");
        }
    }

    public void Stop()
    {
        _cts?.Cancel();
        _listener?.Stop();
    }

    public void Dispose()
    {
        if (_disposed) return;
        _disposed = true;
        Stop();
        _cts?.Dispose();
    }

    private void Log(string message) => LogMessage?.Invoke(message);
}
