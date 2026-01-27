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

package superspreader.beauti;

import bdmmprime.beauti.EpochVisualizerPane;
import bdmmprime.distribution.BirthDeathMigrationDistribution;
import superspreader.parameterization.TimeOnlySkylineParameter;
import beast.base.core.BEASTInterface;
import beast.base.core.Input;
import beast.base.evolution.tree.TraitSet;
import beast.base.evolution.tree.Tree;
import beast.base.inference.parameter.RealParameter;
import beastfx.app.inputeditor.BeautiDoc;
import beastfx.app.inputeditor.InputEditor;
import beastfx.app.util.FXUtils;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.Arrays;
import java.util.stream.Collectors;

public class TimeOnlySkylineInputEditor extends InputEditor.Base {

    TimeOnlySkylineParameter CommonChangeTime;

    VBox mainInputBox;

    EpochVisualizerPane epochVisualizer;

    public TimeOnlySkylineInputEditor(BeautiDoc doc) {
        super(doc);
    }

    @Override
    public Class<?> type() {
        return TimeOnlySkylineParameter.class;
    }

    @Override
    public void init(Input<?> input, BEASTInterface beastObject, int itemNr,
                     ExpandOption isExpandOption, boolean addButtons) {

        m_bAddButtons = addButtons;
        m_input = input;
        m_beastObject = beastObject;
        this.itemNr = itemNr;
        pane = FXUtils.newHBox();

        CommonChangeTime = (TimeOnlySkylineParameter) input.get();

        ensureValuesConsistency();

        addInputLabel();

        // Add elements specific to change times

        int nChanges = CommonChangeTime.getChangeCount();

        mainInputBox = FXUtils.newVBox();
        mainInputBox.setBorder(new Border(new BorderStroke(Color.LIGHTGRAY,
                BorderStrokeStyle.SOLID, null, null)));

        HBox boxHoriz = FXUtils.newHBox();
        Label changePointLabel = new Label("Number of change times:");
        Spinner<Integer> changeCountSpinner = new Spinner<>(0, Integer.MAX_VALUE, nChanges);
        changeCountSpinner.setEditable(true);
        changeCountSpinner.setRepeatDelay(Duration.INDEFINITE); // (Hack around weird race condition I can't solve)
        boxHoriz.getChildren().add(changePointLabel);
        boxHoriz.getChildren().add(changeCountSpinner);

        mainInputBox.getChildren().add(boxHoriz);

        VBox changeTimesBox = FXUtils.newVBox();
        HBox changeTimesEntryRow = FXUtils.newHBox();
        changeTimesBox.getChildren().add(changeTimesEntryRow);
        HBox changeTimesBoxRow = FXUtils.newHBox();
        CheckBox timesAreAgesCheckBox = new CheckBox("Times specified as ages");
        changeTimesBoxRow.getChildren().add(timesAreAgesCheckBox);
        CheckBox estimateTimesCheckBox = new CheckBox("Estimate change times");
        changeTimesBoxRow.getChildren().add(estimateTimesCheckBox);
        changeTimesBox.getChildren().add(changeTimesBoxRow);

        changeTimesBoxRow = FXUtils.newHBox();
        CheckBox timesAreRelativeCheckBox = new CheckBox("Relative to process length");
        changeTimesBoxRow.getChildren().add(timesAreRelativeCheckBox);
        Button distributeChangeTimesButton = new Button("Distribute evenly");
        changeTimesBoxRow.getChildren().add(distributeChangeTimesButton);
        changeTimesBox.getChildren().add(changeTimesBoxRow);

        mainInputBox.getChildren().add(changeTimesBox);

        if (nChanges > 0) {
            updateChangeTimesUI((RealParameter) CommonChangeTime.changeTimesInput.get(),
                    changeTimesEntryRow);
            timesAreAgesCheckBox.setSelected(CommonChangeTime.timesAreAgesInput.get());
            timesAreRelativeCheckBox.setSelected(CommonChangeTime.timesAreRelativeInput.get());

            estimateTimesCheckBox.setSelected(
                    ((RealParameter) CommonChangeTime.changeTimesInput.get())
                            .isEstimatedInput.get());
        } else {
            changeTimesBox.setVisible(false);
            changeTimesBox.setManaged(false);
        }

        boxHoriz = FXUtils.newHBox();
        CheckBox visualizerCheckBox = new CheckBox("Display epoch visualization");
        visualizerCheckBox.setSelected(CommonChangeTime.epochVisualizerDisplayed);
        boxHoriz.getChildren().add(visualizerCheckBox);
        mainInputBox.getChildren().add(boxHoriz);

        epochVisualizer = new EpochVisualizerPane(getTree(), getTypeTraitSet(), CommonChangeTime);
        epochVisualizer.widthProperty().bind(mainInputBox.widthProperty().subtract(10));
        epochVisualizer.setVisible(CommonChangeTime.epochVisualizerDisplayed);
        epochVisualizer.setManaged(CommonChangeTime.epochVisualizerDisplayed);
        mainInputBox.getChildren().add(epochVisualizer);

        epochVisualizer.setScalar(false);

        pane.getChildren().add(mainInputBox);
        getChildren().add(pane);

        // Add event listeners:
        changeCountSpinner.valueProperty().addListener((observable, oldValue, newValue) -> {
            System.out.println(oldValue + " -> " + newValue);

            if (newValue > 0) {
                RealParameter param = (RealParameter) CommonChangeTime.changeTimesInput.get();
                if (param == null) {
                    if (!doc.pluginmap.containsKey(getChangeTimesParameterID())) {
                        param = new RealParameter("0.0");
                        param.setID(getChangeTimesParameterID());
                    } else {
                        param = (RealParameter) doc.pluginmap.get(getChangeTimesParameterID());
                    }
                    CommonChangeTime.changeTimesInput.setValue(param, CommonChangeTime);
                }
                param.setDimension(newValue);
                sanitiseRealParameter(param);
            } else {
                CommonChangeTime.changeTimesInput.setValue(null, CommonChangeTime);
            }

            ensureValuesConsistency();

            if (newValue > 0) {
                updateChangeTimesUI((RealParameter) CommonChangeTime.changeTimesInput.get(),
                        changeTimesEntryRow);
                timesAreAgesCheckBox.setSelected(CommonChangeTime.timesAreAgesInput.get());

                estimateTimesCheckBox.setSelected(
                        ((RealParameter) CommonChangeTime.changeTimesInput.get())
                                .isEstimatedInput.get());

                changeTimesBox.setManaged(true);
                changeTimesBox.setVisible(true);
            } else {
                changeTimesBox.setManaged(false);
                changeTimesBox.setVisible(false);
            }
            refreshPanel();
            System.out.println(CommonChangeTime);

        });

        timesAreAgesCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            CommonChangeTime.timesAreAgesInput.setValue(newValue, CommonChangeTime);
            CommonChangeTime.initAndValidate();
            System.out.println(CommonChangeTime);
            epochVisualizer.repaintCanvas();
        });

        estimateTimesCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            RealParameter changeTimes = (RealParameter) CommonChangeTime.changeTimesInput.get();
            changeTimes.isEstimatedInput.setValue(newValue, changeTimes);
            sync();
        });

        timesAreRelativeCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            CommonChangeTime.timesAreRelativeInput.setValue(newValue, CommonChangeTime);
            CommonChangeTime.initAndValidate();
            System.out.println(CommonChangeTime);
            epochVisualizer.repaintCanvas();
        });


        distributeChangeTimesButton.setOnAction(e -> {

            RealParameter changeTimesParam = (RealParameter) CommonChangeTime.changeTimesInput.get();
            int nTimes = changeTimesParam.getDimension();

            if (CommonChangeTime.timesAreRelativeInput.get()) {
                for (int i = 0; i < nTimes; i++) {
                    changeTimesParam.setValue(i, ((double) (i + 1)) / (nTimes + 1));
                }
            } else {
                if (nTimes > 1) {
                    for (int i = 0; i < nTimes - 1; i++) {
                        changeTimesParam.setValue(i,
                                (changeTimesParam.getArrayValue(nTimes - 1) * (i + 1)) / (nTimes + 1));
                    }
                }
            }

            sanitiseRealParameter(changeTimesParam);
            sync();
        });


        visualizerCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            CommonChangeTime.epochVisualizerDisplayed = newValue;
            epochVisualizer.setVisible(newValue);
            epochVisualizer.setManaged(newValue);
        });

        CommonChangeTime.initAndValidate();

    }


    void ensureValuesConsistency() {
        int nEpochs = CommonChangeTime.changeTimesInput.get() == null
                ? 1
                : CommonChangeTime.changeTimesInput.get().getDimension() + 1;

        System.out.println("Number of epochs: " + nEpochs);

        if (CommonChangeTime.changeTimesInput.get() != null)
            ((RealParameter)CommonChangeTime.changeTimesInput.get()).initAndValidate();
        CommonChangeTime.initAndValidate();
    }


    protected TraitSet getTypeTraitSet() {
        BirthDeathMigrationDistribution bdmmPrimeDistrib =
                (BirthDeathMigrationDistribution) doc.pluginmap.get("BDMMPrime.t:" + getPartitionID());

        return bdmmPrimeDistrib.typeTraitSetInput.get();
    }


    protected Tree getTree() {
        return (Tree) doc.pluginmap.get("Tree.t:" + getPartitionID());
    }


    /**
     * Configure inputs for change times.
     *
     * @param parameter change times parameter
     * @param changeTimesEntryRow HBox containing time inputs
     */
    void updateChangeTimesUI(RealParameter parameter,
                             HBox changeTimesEntryRow) {
        changeTimesEntryRow.getChildren().clear();
        changeTimesEntryRow.getChildren().add(new Label("Change times:"));
        for (int i=0; i<parameter.getDimension(); i++) {
            changeTimesEntryRow.getChildren().add(new Label("Epoch " + (i+1) + "->" + (i+2) + ": "));
            TextField textField = new TextField(parameter.getValue(i).toString());

            textField.setPrefWidth(50);
            textField.setPadding(new Insets(0));
            HBox.setMargin(textField, new Insets(0, 10, 0, 0));

            int index = i;
            textField.textProperty().addListener((observable, oldValue, newValue) -> {
                parameter.setValue(index, Double.valueOf(newValue));
                sanitiseRealParameter(parameter);
                CommonChangeTime.initAndValidate();
                System.out.println(CommonChangeTime);
                epochVisualizer.repaintCanvas();
            });

            changeTimesEntryRow.getChildren().add(textField);
        }
    }

    void sanitiseRealParameter(RealParameter parameter) {
        parameter.valuesInput.setValue(
                Arrays.stream(parameter.getDoubleValues())
                        .mapToObj(String::valueOf)
                        .collect(Collectors.joining(" ")),
                parameter);
        parameter.initAndValidate();
    }


    String getChangeTimesParameterID() {
        int idx = CommonChangeTime.getID().indexOf("SP");
        String prefix = CommonChangeTime.getID().substring(0, idx);
        String suffix = CommonChangeTime.getID().substring(idx+2);

        return prefix + "ChangeTimes" + suffix;
    }

    private String getPartitionID() {
        return CommonChangeTime.getID().split("\\.t:")[1];
    }


    @Override
    public void refreshPanel() {
        sync();
        super.refreshPanel();
    }

}