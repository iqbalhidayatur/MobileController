using System.Net;
using System.Net.Sockets;

namespace MobileGameControllerServer;

/// <summary>
/// Entry point for the Windows PC server.
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

        // Print local IP addresses for easy Android connection setup
        PrintLocalAddresses(port);

        VirtualControllerManager? controllerManager = null;
        ControllerTcpServer? server = null;

        Console.CancelKeyPress += (_, e) =>
        {
            e.Cancel = true;
            Console.WriteLine("\n[Server] Shutting down...");
            server?.Stop();
        };

        try
        {
            controllerManager = new VirtualControllerManager();
            server = new ControllerTcpServer(controllerManager);
            await server.StartAsync(port);
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
        finally
        {
            server?.Dispose();
            controllerManager?.Dispose();
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
        foreach (var ip in Dns.GetHostAddresses(Dns.GetHostName()))
        {
            if (ip.AddressFamily == AddressFamily.InterNetwork)
            {
                Console.ForegroundColor = ConsoleColor.Cyan;
                Console.WriteLine($"  → {ip}:{port}");
                Console.ResetColor();
            }
        }
        Console.WriteLine();
    }
}
