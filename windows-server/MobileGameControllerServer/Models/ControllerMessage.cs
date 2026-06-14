using System.Text.Json.Serialization;

namespace MobileGameControllerServer.Models;

/// <summary>
/// Base message received from Android client over TCP.
/// </summary>
public class ControllerMessage
{
    [JsonPropertyName("type")]
    public string Type { get; set; } = string.Empty;

    [JsonPropertyName("button")]
    public string? Button { get; set; }

    [JsonPropertyName("pressed")]
    public bool Pressed { get; set; }

    [JsonPropertyName("stick")]
    public string? Stick { get; set; }

    [JsonPropertyName("x")]
    public float X { get; set; }

    [JsonPropertyName("y")]
    public float Y { get; set; }

    [JsonPropertyName("timestamp")]
    public long Timestamp { get; set; }
}
