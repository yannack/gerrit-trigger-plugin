/*
 *  The MIT License
 *
 *  Copyright 2012 Sony Mobile Communications AB. All rights reserved.
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
package com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger;

import com.sonyericsson.hudson.plugins.gerrit.trigger.GerritServer;
import com.sonyericsson.hudson.plugins.gerrit.trigger.PluginImpl;
import com.sonymobile.tools.gerrit.gerritevents.GerritDefaultValues;
import hudson.util.TimeUnit2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Timer;

/**
 * Manages the timer that is used for each GerritTrigger TimerTask that
 * is active.
 *
 * @author Fredrik Abrahamson &lt;fredrik.abrahamson@sonymobile.com&gt;
 */
public final class GerritTriggerTimer {

    /**
     * Average number of milliseconds in a second.
     */
    private static final long MILLISECONDS_PER_SECOND = 1000;
    /**
     * Wait this many milliseconds before the first call to a TimerTask
     */
    private static final long DELAY_MILLISECONDS  =  5000;
    private static final Logger logger = LoggerFactory.getLogger(GerritTriggerTimer.class);

    /**
     * The instance used by the singleton mechanism.
     */
    private static GerritTriggerTimer instance = null;

    /**
     * The timer that is doing the actual scheduling.
     */
    private Timer timer = null;

    /**
     * The private constructor (this is a singleton class).
     */
    private GerritTriggerTimer() {
        timer = new Timer(true);
    }

    /**
     * Returns the instance, and creates it first if needed.
     *
     * @return the instance of this class
     */
    public static GerritTriggerTimer getInstance() {
        if (instance == null) {
            instance = new GerritTriggerTimer();
        }
        return instance;
    }

    //CS IGNORE LineLength FOR NEXT 5 LINES. REASON: JavaDoc

    /**
     * Figures out what
     * {@link com.sonyericsson.hudson.plugins.gerrit.trigger.config.IGerritHudsonTriggerConfig#getDynamicConfigRefreshInterval()}
     * to use.
     *
     * @param timerTask the timerTask that should be scheduled.
     *
     * @return the refresh interval in ms.
     * @see #calculateAverageDynamicConfigRefreshInterval()
     */
    private long calculateDynamicConfigRefreshInterval(GerritTriggerTimerTask timerTask) {
        if (timerTask.getGerritTrigger().isAnyServer()) {
            if (PluginImpl.getInstance().getServers() == null || PluginImpl.getInstance().getServers().isEmpty()) {
                return GerritDefaultValues.DEFAULT_DYNAMIC_CONFIG_REFRESH_INTERVAL;
            } else {
                //Do an average just for giggles
                return calculateAverageDynamicConfigRefreshInterval();
            }
        } else {
            //get the actual if it exists.
            GerritServer server = PluginImpl.getInstance().getServer(timerTask.getGerritTrigger().getServerName());
            if (server != null) {
                return server.getConfig().getDynamicConfigRefreshInterval();
            } else {
                //Do an average just for giggles
                return calculateAverageDynamicConfigRefreshInterval();
            }
        }
    }

    //CS IGNORE LineLength FOR NEXT 5 LINES. REASON: JavaDoc

    /**
     * Calculates the average
     * {@link com.sonyericsson.hudson.plugins.gerrit.trigger.config.IGerritHudsonTriggerConfig#getDynamicConfigRefreshInterval()}
     * among the configured GerritServers.
     *
     * @return the average value.
     */
    private long calculateAverageDynamicConfigRefreshInterval() {
        long total = 0;
        for (GerritServer server : PluginImpl.getInstance().getServers()) {
            total += server.getConfig().getDynamicConfigRefreshInterval();
        }
        long average = total / PluginImpl.getInstance().getServers().size();
        return Math.max(GerritDefaultValues.MINIMUM_DYNAMIC_CONFIG_REFRESH_INTERVAL, average);
    }

    /**
     * Schedule a TimerTask according to the two constants above.
     *
     * @param timerTask the TimerTask to be scheduled
     */
    public void schedule(GerritTriggerTimerTask timerTask) {
        long timerPeriod = TimeUnit2.SECONDS.toMillis(calculateDynamicConfigRefreshInterval(timerTask));
        try {
            timer.schedule(timerTask, DELAY_MILLISECONDS, timerPeriod);
        } catch (IllegalArgumentException iae) {
            logger.error("Attempted use of negative delay", iae);
        } catch (IllegalStateException ise) {
            logger.error("Attempted re-use of TimerTask", ise);
        }
    }
}
