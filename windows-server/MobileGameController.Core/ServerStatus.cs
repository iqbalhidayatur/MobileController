namespace MobileGameController.Core;

public enum ServerRunState
{
    Stopped,
    Starting,
    Listening,
    ClientConnected,
    Error
}

public sealed class ServerStatus
{
    public ServerRunState State { get; init; } = ServerRunState.Stopped;
    public int Port { get; init; }
    public string? ConnectedClient { get; init; }
    public string? ErrorMessage { get; init; }
    public bool IsViGEmReady { get; init; }
}
