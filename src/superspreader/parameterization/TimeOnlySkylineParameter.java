/*
 * Copyright (C) 2026 Institut Pasteur PARIS
 *
 * This file is part of the SuperSpreader BEAST module project.
 *
 * SuperSpreader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * See the COPYING file for details.
 */

package superspreader.parameterization;

import bdmmprime.parameterization.SkylineParameter;
import beast.base.core.Input;
import java.io.PrintStream;


/**
 * A Skyline parameter with no values, only the time changes. It extends the SkylineParameter class in order to benefit
 * from the epochVisualizer in its beauti editor.
 */
public class TimeOnlySkylineParameter extends SkylineParameter {


    public TimeOnlySkylineParameter() {
        skylineValuesInput.setRule(Input.Validate.FORBIDDEN);
    }


    @Override
    public void initAndValidate() {

        super.initAndValidate();

        if (typeSetInput.get() != null) {
            nTypes = typeSetInput.get().getNTypes();

        } else {
            nTypes = 1;
        }

    }


    @Override
    protected void updateValues() {
        // Nothing to do here
    }

    @Override
    public void init(PrintStream out) {

        for (int interval = 0; interval < nIntervals; interval++) {

            if (interval < nIntervals - 1)
                out.print(getID() + "i" + interval + "_endtime\t");

            out.print("\t");

        }
    }

    @Override
    public void close(PrintStream out) {
    }

    @Override
    public void log(long sample, PrintStream out) {

        updateTimes();

        for (int interval = 0; interval < nIntervals; interval++) {

            if (interval < nIntervals - 1)
                out.print(times[interval] + "\t");

        }
    }

    @Override
    public String toString() {

        updateTimes();

        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());

        sb.append(":");
        for (int i = 0; i < getChangeCount() + 1; i++) {
            if (i > 0)
                sb.append(" (change time ").append(times[i - 1]).append(")");
        }

        return sb.toString();
    }

}
