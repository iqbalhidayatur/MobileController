using Nefarius.ViGEm.Client;
using Nefarius.ViGEm.Client.Targets;
using Nefarius.ViGEm.Client.Targets.Xbox360;

namespace MobileGameController.Core;

/// <summary>
/// Manages a virtual Xbox 360 controller via ViGEmBus driver.
/// Maps Android controller events to Xbox 360 button/stick states.
/// </summary>
public class VirtualControllerManager : IDisposable
{
    private readonly ViGEmClient _client;
    private readonly IXbox360Controller _controller;
    private bool _disposed;

    public VirtualControllerManager()
    {
        _client = new ViGEmClient();
        _controller = _client.CreateXbox360Controller();
        _controller.Connect();
    }

    public void HandleButton(string button, bool pressed)
    {
        var xboxButton = MapButton(button);
        if (xboxButton.HasValue)
        {
            _controller.SetButtonState(xboxButton.Value, pressed);
            _controller.SubmitReport();
        }
    }

    public void HandleAnalog(string stick, float x, float y)
    {
        short axisX = (short)(x * 32767);
        short axisY = (short)(y * 32767);

        if (stick.Equals("left", StringComparison.OrdinalIgnoreCase))
        {
            _controller.SetAxisValue(Xbox360Axis.LeftThumbX, axisX);
            _controller.SetAxisValue(Xbox360Axis.LeftThumbY, axisY);
        }
        else if (stick.Equals("right", StringComparison.OrdinalIgnoreCase))
        {
            _controller.SetAxisValue(Xbox360Axis.RightThumbX, axisX);
            _controller.SetAxisValue(Xbox360Axis.RightThumbY, axisY);
        }

        _controller.SubmitReport();
    }

    public void HandleTrigger(string button, bool pressed)
    {
        byte value = pressed ? (byte)255 : (byte)0;
        if (button.Equals("LT", StringComparison.OrdinalIgnoreCase))
        {
            _controller.SetSliderValue(Xbox360Slider.LeftTrigger, value);
        }
        else if (button.Equals("RT", StringComparison.OrdinalIgnoreCase))
        {
            _controller.SetSliderValue(Xbox360Slider.RightTrigger, value);
        }
        _controller.SubmitReport();
    }

    private static Xbox360Button? MapButton(string button) => button.ToUpperInvariant() switch
    {
        "A" => Xbox360Button.A,
        "B" => Xbox360Button.B,
        "X" => Xbox360Button.X,
        "Y" => Xbox360Button.Y,
        "START" => Xbox360Button.Start,
        "SELECT" => Xbox360Button.Back,
        "LB" => Xbox360Button.LeftShoulder,
        "RB" => Xbox360Button.RightShoulder,
        "DPAD_UP" => Xbox360Button.Up,
        "DPAD_DOWN" => Xbox360Button.Down,
        "DPAD_LEFT" => Xbox360Button.Left,
        "DPAD_RIGHT" => Xbox360Button.Right,
        "LT" => null,
        "RT" => null,
        _ => null
    };

    public void ResetController()
    {
        _controller.ResetButtonState();
        _controller.SetAxisValue(Xbox360Axis.LeftThumbX, 0);
        _controller.SetAxisValue(Xbox360Axis.LeftThumbY, 0);
        _controller.SetAxisValue(Xbox360Axis.RightThumbX, 0);
        _controller.SetAxisValue(Xbox360Axis.RightThumbY, 0);
        _controller.SetSliderValue(Xbox360Slider.LeftTrigger, 0);
        _controller.SetSliderValue(Xbox360Slider.RightTrigger, 0);
        _controller.SubmitReport();
    }

    public void Dispose()
    {
        if (_disposed) return;
        _disposed = true;
        _controller.Disconnect();
        _client.Dispose();
    }
}
