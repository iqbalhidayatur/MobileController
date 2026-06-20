using System.Net.Sockets;
using MobileGameController.Core;

namespace MobileGameControllerServer;

/// <summary>
/// Console entry point for the Windows PC server.
/// Requires ViGEmBus driver to be installed.
/// </summary>
class Program
{
    private const int DefaultPort = 27015;

    static async Task Main(string[] args)
    {
        Console.Title = "Mobile Game Controller - PC Server";
        PrintBanner();

        int port = DefaultPort;
        if (args.Length > 0 && int.TryParse(args[0], out var customPort))
        {
            port = customPort;
        }

        PrintLocalAddresses(port);

        using var host = new ServerHost();
        host.LogMessage += message => Console.WriteLine($"[Server] {message}");

        Console.CancelKeyPress += (_, e) =>
        {
            e.Cancel = true;
            Console.WriteLine("\n[Server] Shutting down...");
            _ = host.StopAsync();
        };

        try
        {
            await host.StartAsync(port);
        }
        catch (Exception ex) when (ex.Message.Contains("ViGEm") || ex.GetType().Name.Contains("ViGEm"))
        {
            Console.ForegroundColor = ConsoleColor.Red;
            Console.WriteLine("\n[ERROR] ViGEmBus driver not found!");
            Console.WriteLine("Please install ViGEmBus from: https://github.com/ViGEm/ViGEmBus/releases");
            Console.ResetColor();
        }
        catch (SocketException ex)
        {
            Console.ForegroundColor = ConsoleColor.Red;
            Console.WriteLine($"\n[ERROR] Cannot bind to port {port}: {ex.Message}");
            Console.WriteLine("Try a different port or check if another instance is running.");
            Console.ResetColor();
        }
        catch (Exception ex)
        {
            Console.ForegroundColor = ConsoleColor.Red;
            Console.WriteLine($"\n[ERROR] {ex.Message}");
            Console.ResetColor();
        }

        Console.WriteLine("[Server] Press any key to exit...");
        Console.ReadKey();
    }

    static void PrintBanner()
    {
        Console.ForegroundColor = ConsoleColor.Green;
        Console.WriteLine(@"
  ╔══════════════════════════════════════════╗
  ║   Mobile Game Controller - PC Server     ║
  ║   Virtual Xbox 360 via ViGEmBus          ║
  ╚══════════════════════════════════════════╝");
        Console.ResetColor();
    }

    static void PrintLocalAddresses(int port)
    {
        Console.WriteLine("\n[INFO] Connect your Android app to one of these addresses:\n");
        foreach (var ip in NetworkHelper.GetLocalIPv4Addresses())
        {
            Console.ForegroundColor = ConsoleColor.Cyan;
            Console.WriteLine($"  → {ip}:{port}");
            Console.ResetColor();
        }
        Console.WriteLine();
    }
}
