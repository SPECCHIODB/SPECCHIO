package ch.specchio.gui;

import ch.specchio.plots.swing.SpectralPlot;

import javax.swing.*;

public class VisPanel extends JPanel {
    private SpectralPlot myPlot;

    public VisPanel(){
    }

    public void appendPlotArea(SpectralPlot spectralPlot){
        this.myPlot = spectralPlot;
    }
}
