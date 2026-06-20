using System.Net;
using System.Net.Sockets;

namespace MobileGameController.Core;

public static class NetworkHelper
{
    public static IReadOnlyList<string> GetLocalIPv4Addresses()
    {
        var addresses = new List<string>();

        foreach (var ip in Dns.GetHostAddresses(Dns.GetHostName()))
        {
            if (ip.AddressFamily == AddressFamily.InterNetwork)
            {
                addresses.Add(ip.ToString());
            }
        }

        return addresses;
    }
}
