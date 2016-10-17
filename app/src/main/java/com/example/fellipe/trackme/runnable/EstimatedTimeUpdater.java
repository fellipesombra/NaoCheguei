package com.example.fellipe.trackme.runnable;

import android.os.Handler;
import android.widget.TextView;

import com.example.fellipe.trackme.service.MapService;

/**
 * Created by Fellipe on 16/10/2016.
 */

public class EstimatedTimeUpdater implements Runnable{

    private MapService mapService;
    private Handler handler;
    private TextView timeText;

    public EstimatedTimeUpdater(MapService mapService, Handler handler, TextView timeText) {
        this.mapService = mapService;
        this.handler = handler;
        this.timeText = timeText;
    }

    @Override
    public void run() {
        mapService.decreaseActualEstimatedTime(1);
        timeText.setText(mapService.getActualEstimatedTimeText());
        handler.postDelayed(this,1000);
    }
}
