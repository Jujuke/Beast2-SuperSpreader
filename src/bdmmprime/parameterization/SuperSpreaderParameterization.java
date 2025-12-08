package bdmmprime.parameterization;

import beast.base.core.Input;

public class SuperSpreaderParameterization extends Parameterization {

    public Input<SkylineVectorParameter> becomeUninfectiousRateInput = new Input<>("becomeUninfectiousRate",
            "Become uninfectious rate skyline.", Input.Validate.REQUIRED);

    public Input<SkylineVectorParameter> samplingProportionInput = new Input<>("samplingProportion",
            "Sampling proportion skyline.", Input.Validate.REQUIRED);

    public Input<TimedParameter> rhoSamplingInput = new Input<>("rhoSampling",
            "Contemporaneous sampling times and probabilities.");

    public Input<SkylineVectorParameter> removalProbInput = new Input<>("removalProb",
            "Removal prob skyline.", Input.Validate.REQUIRED);

    public Input<SkylineMatrixParameter> migRateInput = new Input<>("migrationRate",
            "Migration rate skyline.");

    public Input<TimeOnlySkylineParameter> CommonChangeTimeInput = new Input<>("CommonChangeTime",
            "Reproduction number multiplier skyline.", Input.Validate.REQUIRED);

    public Input<SkylineScalarCommonTimeParameter> ReAverageInput = new Input<>("ReAverage",
            "Reproduction number average skyline.", Input.Validate.REQUIRED);

    public Input<SkylineScalarCommonTimeParameter> SSFracInput = new Input<>("SSFrac",
            "Super-spreader fraction skyline.", Input.Validate.REQUIRED);

    public Input<SkylineScalarCommonTimeParameter> ReMultiplierInput = new Input<>("ReMultiplier",
            "Reproduction number multiplier skyline.", Input.Validate.REQUIRED);

    @Override
    public double[] getMigRateChangeTimes() {
        if (migRateInput.get() == null)
            return EMPTY_TIME_ARRAY;

        return migRateInput.get().getChangeTimes();
    }

    private double[] birthRateChangeTimes;

    @Override
    public double[] getBirthRateChangeTimes() {
        birthRateChangeTimes = combineAndSortTimes(birthRateChangeTimes,
                ReAverageInput.get().getChangeTimes(),
                SSFracInput.get().getChangeTimes(),
                ReMultiplierInput.get().getChangeTimes(),
                becomeUninfectiousRateInput.get().getChangeTimes());

        return birthRateChangeTimes;
    }


    private double[] crossBirthRateChangeTimes;

    @Override
    public double[] getCrossBirthRateChangeTimes() {
        crossBirthRateChangeTimes = combineAndSortTimes(crossBirthRateChangeTimes,
                ReAverageInput.get().getChangeTimes(),
                SSFracInput.get().getChangeTimes(),
                ReMultiplierInput.get().getChangeTimes(),
                becomeUninfectiousRateInput.get().getChangeTimes());

        return crossBirthRateChangeTimes;
    }


    private double[] deathRateChangeTimes;

    @Override
    public double[] getDeathRateChangeTimes() {
        deathRateChangeTimes = combineAndSortTimes(deathRateChangeTimes,
                becomeUninfectiousRateInput.get().getChangeTimes(),
                samplingProportionInput.get().getChangeTimes(),
                removalProbInput.get().getChangeTimes());

        return deathRateChangeTimes;
    }

    private double[] samplingRateChangeTimes;

    @Override
    public double[] getSamplingRateChangeTimes() {
        samplingRateChangeTimes = combineAndSortTimes(samplingRateChangeTimes,
                samplingProportionInput.get().getChangeTimes(),
                becomeUninfectiousRateInput.get().getChangeTimes());

        return samplingRateChangeTimes;
    }

    @Override
    public double[] getRemovalProbChangeTimes() {
        return removalProbInput.get().getChangeTimes();
    }

    @Override
    public double[] getRhoSamplingTimes() {
        if (rhoSamplingInput.get() == null)
            return EMPTY_TIME_ARRAY;

        return rhoSamplingInput.get().getTimes();
    }

    @Override
    protected double[][] getMigRateValues(double time) {
        if (migRateInput.get() == null)
            return ZERO_VALUE_MATRIX;

        return migRateInput.get().getValuesAtTime(time);
    }

    @Override
    protected double[] getBirthRateValues(double time) {
        double reAv = ReAverageInput.get().getValuesAtTime(time);
        double ssFrac = SSFracInput.get().getValuesAtTime(time);
        double reMultiplier = ReMultiplierInput.get().getValuesAtTime(time);
        double reNS = reAv/ (1 - ssFrac + ssFrac * reMultiplier);
        double[] res = new double[2];
        res[0] = reNS * (1 - ssFrac);
        res[1] = reNS * reMultiplier * ssFrac;

        double[] buVals = becomeUninfectiousRateInput.get().getValuesAtTime(time);

        for (int type=0; type<nTypes; type++)
            res[type] *= buVals[type];

        return res;
    }

    @Override
    protected double[][] getCrossBirthRateValues(double time) {
        double reAv = ReAverageInput.get().getValuesAtTime(time);
        double ssFrac = SSFracInput.get().getValuesAtTime(time);
        double reMultiplier = ReMultiplierInput.get().getValuesAtTime(time);
        double[][] res = new double[2][2];
        double reNS = reAv/ (1 - ssFrac + ssFrac * reMultiplier);
        res[0][1] = reNS * ssFrac;
        res[1][0] = reNS * reMultiplier * (1 - ssFrac);

        double[] buVals = becomeUninfectiousRateInput.get().getValuesAtTime(time);

        for (int sourceType=0; sourceType<nTypes; sourceType++) {
            for (int destType=0; destType<nTypes; destType++) {
                if (sourceType==destType)
                    continue;

                res[sourceType][destType] *= buVals[sourceType];
            }
        }

        return res;
    }

    @Override
    protected double[] getDeathRateValues(double time) {
        double[] res = becomeUninfectiousRateInput.get().getValuesAtTime(time);
        double[] samplingProp = samplingProportionInput.get().getValuesAtTime(time);
        double[] removalProb = removalProbInput.get().getValuesAtTime(time);

        for (int type=0; type<nTypes; type++)
            res[type] *= (1 - samplingProp[type])
                    / (1.0 - (1.0-removalProb[type])*samplingProp[type]);

        return res;
    }

    @Override
    protected double[] getSamplingRateValues(double time) {
        double[] res = samplingProportionInput.get().getValuesAtTime(time);
        double[] buRate  = becomeUninfectiousRateInput.get().getValuesAtTime(time);
        double[] removalProb  = removalProbInput.get().getValuesAtTime(time);

        for (int type=0; type<nTypes; type++)
            res[type] = res[type]*buRate[type]/(1 - (1-removalProb[type])*res[type]);

        return res;
    }

    @Override
    protected double[] getRemovalProbValues(double time) {
        return removalProbInput.get().getValuesAtTime(time);
    }

    @Override
    protected double[] getRhoValues(double time) {
        if (rhoSamplingInput.get() != null)
            return rhoSamplingInput.get().getValuesAtTime(time);
        else
            return ZERO_VALUE_ARRAY;
    }

    @Override
    protected void validateParameterTypeCounts() {
        if (ReAverageInput.get().getNTypes() != 1)
            throw new IllegalArgumentException("ReAverage type count should be 1 (scalar parameter).");

        if (SSFracInput.get().getNTypes() != 1)
            throw new IllegalArgumentException("SSfrac type count should be 1 (scalar parameter).");

        if (ReMultiplierInput.get().getNTypes() != 1)
            throw new IllegalArgumentException("ReMultiplier type count should be 1 (scalar parameter).");

        if (becomeUninfectiousRateInput.get().getNTypes() != getNTypes())
            throw new IllegalArgumentException("Become uninfectious rate skyline type count does not match type count of model.");

        if (samplingProportionInput.get().getNTypes() != getNTypes())
            throw new IllegalArgumentException("Sampling proportion skyline type count does not match type count of model.");

        if (migRateInput.get() != null
                && migRateInput.get().getNTypes() != getNTypes())
            throw new IllegalArgumentException("Migration rate skyline type count does not match type count of model.");

        if (removalProbInput.get().getNTypes() != getNTypes())
            throw new IllegalArgumentException("Removal prob skyline type count does not match type count of model.");

        if (rhoSamplingInput.get() != null
                && rhoSamplingInput.get().getNTypes() != getNTypes())
            throw new IllegalArgumentException("Rho sampling type count does not match type count of model.");
    }
}
