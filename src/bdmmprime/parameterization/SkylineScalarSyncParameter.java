/*
 * Copyright (C) 2019-2024 ETH Zurich
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package bdmmprime.parameterization;

import beast.base.inference.parameter.RealParameter;

import java.io.PrintStream;
import java.util.Arrays;

public class SkylineScalarSyncParameter extends SkylineParameter {

    double[] values, storedValues;
    double valuesAtTime;

//    boolean inputIsScalar;

    public SkylineScalarSyncParameter() { }

    public SkylineScalarSyncParameter(RealParameter changeTimesParam,
                                      RealParameter skylineValuesParam) {
        super(changeTimesParam, skylineValuesParam);
    }

//    public SkylineScalarParameter(RealParameter changeTimesParam,
//                                  RealParameter skylineValuesParam,
//                                  int nTypes) {
//        super(changeTimesParam, skylineValuesParam, nTypes, null);
//    }

    public SkylineScalarSyncParameter(RealParameter changeTimesParam,
                                      RealParameter skylineValuesParam,
                                      RealParameter origin) {
        super(changeTimesParam, skylineValuesParam, 1, origin);
    }

    @Override
    public void initAndValidate() {
        super.initAndValidate();

        if (skylineValuesInput.get().getDimension() % nIntervals != 0)
            throw new IllegalArgumentException("Value parameter dimension must " +
                    "be a multiple of the number of intervals.");

        nTypes = 1;

        values = new double[nIntervals];
        storedValues = new double[nIntervals];

    }

    @Override
    protected void updateValues() {

        for (int interval=0; interval<nIntervals; interval++) {
            values[interval] = skylineValuesInput.get().getArrayValue(interval);
        }

    }

    /**
     * Retrieve value of vector at a chosen time (not age).
     *
     * @param time when to evaluate the skyline parameter.
     * @return value of the vector at the chosen time.
     */
    public double getValuesAtTime(double time) {
        update();

        int intervalIdx = getIntervalIdx(time);

        valuesAtTime = values[intervalIdx];

        return valuesAtTime;
    }

    public int getNTypes() {
        return nTypes;
    }

    @Override
    protected void store() {
        super.store();

        if (nIntervals >= 0) System.arraycopy(values, 0, storedValues, 0, nIntervals);
    }

    @Override
    protected void restore() {
        super.restore();

        double [] tmp;
        tmp = values;
        values = storedValues;
        storedValues = tmp;
    }

    /*
     * Loggable implementation
     */

    @Override
    public void init(PrintStream out) {

        for (int interval=0; interval<nIntervals; interval++) {

            if (interval < nIntervals-1)
                out.print(getID() + "i" + interval + "_endtime\t");

            out.print(getID());

            if (nIntervals > 1)
                out.print("i" + interval);

            out.print("\t");

        }
    }

    @Override
    public void log(long sample, PrintStream out) {

        for (int interval=0; interval<nIntervals; interval++) {

            if (interval<nIntervals-1)
                out.print(times[interval] + "\t");

            out.print(values[interval] + "\t");
        }
    }

    @Override
    public void close(PrintStream out) {
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());

        sb.append(":");
        for (int i=0; i<getChangeCount()+1; i++) {
            if (i>0)
                sb.append(" (change time ").append(times[i-1]).append(")");
            sb.append(" ").append(Arrays.toString(values));
        }

        return sb.toString();
    }
}
