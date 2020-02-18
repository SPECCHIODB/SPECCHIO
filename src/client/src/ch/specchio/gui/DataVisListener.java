package ch.specchio.gui;

import java.util.EventListener;

public interface DataVisListener extends EventListener {
    public void dataVisEventOccurred(DataVisEvent e);
}
