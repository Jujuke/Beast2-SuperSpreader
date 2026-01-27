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

import bdmmprime.parameterization.TypeSet;
import bdmmprime.util.Utils;
import beast.base.core.Function;
import beast.base.core.Input;
import beast.base.core.Loggable;
import beast.base.inference.CalculationNode;

import java.io.PrintStream;
import java.util.Arrays;

/**
 * This class is a simple case of skyline parameter that is always scalar and with time change given by a shared Input
 * parameter.
 * This class reuses and adapts logic from {@link bdmmprime.parameterization.SkylineParameter},
 * and {@link bdmmprime.parameterization.SkylineVectorParameter}
 */
public class SkylineScalarWithCommonTimeParameter extends CalculationNode implements Loggable {

    public Input<TimeOnlySkylineParameter> commonSkylineInput = new Input<>("commonSkyline",
            "shared Skyline times for the super spreader parameterization", Input.Validate.REQUIRED);

    public Input<Function> skylineValuesInput = new Input<>("skylineValues",
            "Parameter specifying parameter values through time.",
            Input.Validate.REQUIRED);

    public Input<Function> processLengthInput = new Input<>("processLength",
            "Time between start of process and the end.");

    public Input<TypeSet> typeSetInput = new Input<>("typeSet",
            "Type set defining distinct types in model. Used when a" +
                    "single value is to be shared amongst several types.");

    public Input<Boolean> linkIdenticalValuesInput = new Input<>("linkIdenticalValues",
            "BEAUti hint to create XMLs in which identical values are considered " +
                    "linked. WARNING: The value of this input usually has no impact " +
                    "outside of BEAUti!",
            false);

    public boolean timesAreAges, timesAreRelative;

    public double[] times, storedTimes;

    public int nIntervals, nTypes;

    public boolean isDirty;


    double[] values, storedValues;
    double valuesAtTime;

    public SkylineScalarWithCommonTimeParameter() {
    }


    @Override
    public void initAndValidate() {

        timesAreAges = commonSkylineInput.get().timesAreAgesInput.get();
        timesAreRelative = commonSkylineInput.get().timesAreRelativeInput.get();
        int nChangeTimes = commonSkylineInput.get().changeTimesInput.get() == null ? 0 : commonSkylineInput.get().changeTimesInput.get().getDimension();
        nIntervals = nChangeTimes + 1;


        if ((timesAreAges || timesAreRelative) && processLengthInput.get() == null)
            throw new IllegalArgumentException("Process length parameter or tree must be supplied " +
                    "when times are given as ages and/or when times are relative.");


        times = new double[nIntervals - 1];

        storedTimes = new double[nIntervals - 1];

        if (skylineValuesInput.get().getDimension() % nIntervals != 0)
            throw new IllegalArgumentException("Value parameter dimension must " +
                    "be a multiple of the number of intervals.");

        nTypes = 1; //JKE

        values = new double[nIntervals];

        storedValues = new double[nIntervals];

        isDirty = true;
    }

    public int getNTypes() {

        return nTypes;
    }

    /**
     * @return times (not ages) when parameter changes.
     */
    public double[] getChangeTimes() {
        update();

        return times;
	}

	public int getChangeCount() {
        update();

        return times.length;
    }


	protected void update() {
	    if (!isDirty)
	        return;

	    updateTimes();
	    updateValues();

        isDirty = false;
    }

    protected void updateTimes() {

 	    if (nIntervals==1)
	        return;

        for (int i = 0; i < nIntervals - 1; i++)
            times[i] = commonSkylineInput.get().changeTimesInput.get().getArrayValue(i);


        if (timesAreRelative) {

            double startAge = processLengthInput.get().getArrayValue();

            for (int i = 0; i < times.length; i++)
                times[i] *= startAge;
        }

        if (timesAreAges) {
            Utils.reverseDoubleArray(times);

            double startAge = processLengthInput.get().getArrayValue();

            for (int i = 0; i < times.length; i++)
                times[i] = startAge - times[i];
        }
    }

    protected void updateValues() {

        for (int interval = 0; interval < nIntervals; interval++) {
            values[interval] = skylineValuesInput.get().getArrayValue(interval);
        }

        if (timesAreAges)
            Utils.reverseDoubleArray(values);
    }


    protected int getIntervalIdx(double time) {
        if (nIntervals==1)
            return 0;

		int idx = Arrays.binarySearch(times, time);

		if (idx < 0)
			idx = -idx - 1;

		return idx;
    }

    @Override
    protected void restore() {
        super.restore();

        double[] tmp;

        if (nIntervals>1) {
            tmp = times;
            times = storedTimes;
            storedTimes = tmp;
        }

        tmp = values;
        values = storedValues;
        storedValues = tmp;

        isDirty = false;
    }

    @Override
    protected void store() {
        super.store();

        if (nIntervals>1)
            System.arraycopy(times, 0, storedTimes, 0, times.length);

        if (nIntervals >= 0) System.arraycopy(values, 0, storedValues, 0, nIntervals);
    }

    @Override
    protected boolean requiresRecalculation() {
        isDirty = true;
        return true;
    }

    public boolean epochVisualizerDisplayed = false;


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


    /*
     * Loggable implementation
     */

    @Override
    public void init(PrintStream out) {

        for (int interval = 0; interval < nIntervals; interval++) {

            out.print(getID());

            if (nIntervals > 1)
                out.print("i" + interval);

            out.print("\t");

        }
    }

    @Override
    public void log(long sample, PrintStream out) {

        for (int interval = 0; interval < nIntervals; interval++) {
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
        for (int i = 0; i < getChangeCount() + 1; i++) {
            if (i > 0)
                sb.append(" (change time ").append(times[i - 1]).append(")");
            sb.append(" ").append(Arrays.toString(values));
        }

        return sb.toString();
    }
}
