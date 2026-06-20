using System.Windows;
using MobileGameControllerDesktop.ViewModels;

namespace MobileGameControllerDesktop;

public partial class MainWindow : Window
{
    private readonly MainViewModel _viewModel = new();

    public MainWindow()
    {
        InitializeComponent();
        DataContext = _viewModel;
        Closed += (_, _) => _viewModel.Dispose();
    }
}
