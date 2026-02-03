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

import bdmmprime.distribution.BirthDeathMigrationDistribution;
import superspreader.parameterization.SkylineScalarWithCommonTimeParameter;
import superspreader.parameterization.SuperSpreaderParameterization;
import bdmmprime.parameterization.TypeSet;
import beast.base.core.BEASTInterface;
import beast.base.core.Function;
import beast.base.core.Input;
import beast.base.evolution.tree.TraitSet;
import beast.base.evolution.tree.Tree;
import beast.base.inference.parameter.RealParameter;
import beastfx.app.inputeditor.BeautiDoc;
import beastfx.app.inputeditor.InputEditor;
import beastfx.app.util.FXUtils;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValueBase;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.converter.DoubleStringConverter;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * This class reuses and adapts code from {@link bdmmprime.beauti.SkylineInputEditor}
 * and {@link bdmmprime.beauti.SkylineVectorInputEditor}
 */
public class SkylineScalarWithCommonTimeInputEditor extends InputEditor.Base {

    SkylineScalarWithCommonTimeParameter skylineScalarWithCommonTimeParameter;

    public TableView<ValuesTableEntry> valuesTable;

    public VBox mainInputBox;

    public SkylineScalarWithCommonTimeInputEditor(BeautiDoc doc) {
        super(doc);
    }

    @Override
    public Class<?> type() {
        return SkylineScalarWithCommonTimeParameter.class;
    }

    @Override
    public void init(Input<?> input, BEASTInterface beastObject, int itemNr,
                     ExpandOption isExpandOption, boolean addButtons) {

        skylineScalarWithCommonTimeParameter = (SkylineScalarWithCommonTimeParameter) input.get();

        m_input = input;

        m_bAddButtons = addButtons;
        m_beastObject = beastObject;
        this.itemNr = itemNr;
        pane = FXUtils.newHBox();

        ensureValuesConsistency();

        addInputLabel();

        // Add elements specific to change times

        mainInputBox = FXUtils.newVBox();
        mainInputBox.setBorder(new Border(new BorderStroke(Color.LIGHTGRAY,
                BorderStrokeStyle.SOLID, null, null)));

        HBox boxHoriz;

        // Add elements specific to values

        RealParameter valuesParameter = (RealParameter) skylineScalarWithCommonTimeParameter.skylineValuesInput.get();

        boxHoriz = FXUtils.newHBox();
        boxHoriz.getChildren().add(new Label("Values:"));
        valuesTable = new TableView<>();
        valuesTable.getSelectionModel().setCellSelectionEnabled(true);
        valuesTable.setEditable(true);
        VBox valuesTableBoxCol = FXUtils.newVBox();
        valuesTableBoxCol.getChildren().add(valuesTable);
        boxHoriz.getChildren().add(valuesTableBoxCol);

        mainInputBox.getChildren().add(boxHoriz);

        boxHoriz = FXUtils.newHBox();
        CheckBox estimateValuesCheckBox = new CheckBox("Estimate values");
        estimateValuesCheckBox.setSelected(valuesParameter.isEstimatedInput.get());
        boxHoriz.getChildren().add(estimateValuesCheckBox);
        CheckBox linkValuesCheckBox = new CheckBox("Link identical values");
        linkValuesCheckBox.setSelected(skylineScalarWithCommonTimeParameter.linkIdenticalValuesInput.get());
        boxHoriz.getChildren().add(linkValuesCheckBox);
        mainInputBox.getChildren().add(boxHoriz);

        pane.getChildren().add(mainInputBox);
        getChildren().add(pane);

        estimateValuesCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            valuesParameter.isEstimatedInput.setValue(newValue, valuesParameter);
            sync();
        });

        linkValuesCheckBox.selectedProperty().addListener((observable, oldValue, newValue) -> {
            skylineScalarWithCommonTimeParameter.linkIdenticalValuesInput.setValue(newValue, beastObject);
            sync();
        });

        skylineScalarWithCommonTimeParameter.initAndValidate();

        updateValuesUI();

        valuesTable.setFixedCellSize(25);
        valuesTable.prefHeightProperty().bind(valuesTable.fixedCellSizeProperty()
                .multiply(Bindings.size(valuesTable.getItems()).add(1.1)));

        refreshPanel();
    }

    void ensureValuesConsistency() {

        Input<Function> changeTimesInput;
        changeTimesInput = skylineScalarWithCommonTimeParameter.commonSkylineInput.get().changeTimesInput;


        int nEpochs = changeTimesInput.get() == null
                ? 1
                : changeTimesInput.get().getDimension() + 1;

        RealParameter valuesParam = (RealParameter) skylineScalarWithCommonTimeParameter.skylineValuesInput.get();

        System.out.println("Number of epochs: " + nEpochs);

        valuesParam.setDimension(nEpochs);

        if (changeTimesInput.get() != null)
            ((RealParameter) changeTimesInput.get()).initAndValidate();
        sanitiseRealParameter(valuesParam);
        skylineScalarWithCommonTimeParameter.initAndValidate();
    }

    public void sanitiseRealParameter(RealParameter parameter) { //JKE temp
        parameter.valuesInput.setValue(
                Arrays.stream(parameter.getDoubleValues())
                        .mapToObj(String::valueOf)
                        .collect(Collectors.joining(" ")),
                parameter);
        parameter.initAndValidate();
    }


    void updateValuesUI() {
        valuesTable.getColumns().clear();
        valuesTable.getItems().clear();

        int nChanges = skylineScalarWithCommonTimeParameter.getChangeCount();

        RealParameter valuesParameter = (RealParameter) skylineScalarWithCommonTimeParameter.skylineValuesInput.get();
        TableColumn<ValuesTableEntry, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(p -> new ObservableValueBase<>() {
            @Override
            public String getValue() {
                int type = ((VectorValuesEntry) p.getValue()).type;
                return type < 0
                        ? "ALL"
                        : skylineScalarWithCommonTimeParameter.typeSetInput.get().getTypeName(type);
            }
        });
        valuesTable.getColumns().add(typeCol);
        for (int i=0; i<nChanges+1; i++) {
            TableColumn<ValuesTableEntry, Double> col = new TableColumn<>("Epoch " + (i+1));
            int epochIdx = i;
            col.setCellValueFactory(p -> new ObservableValueBase<>() {
                @Override
                public Double getValue() {
                    return valuesParameter.getValue(epochIdx);
                }
            });
            col.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
            col.setOnEditCommit(e -> {

                valuesParameter.setValue(epochIdx, e.getNewValue());

                sanitiseRealParameter(valuesParameter);
                refreshPanel();
            });
            valuesTable.getColumns().add(col);
        }

        valuesTable.getItems().add(new VectorValuesEntry(-1));
    }

    public static class VectorValuesEntry extends ValuesTableEntry {
        public int type;

        public VectorValuesEntry(int type) {
            this.type = type;
        }
    }

    /**
     * Hack to update the type trait set panel and the equilibrium frequencies..
     */
    void updateFrequenciesForSSFrac() {

        for (BEASTInterface beastInterface : skylineScalarWithCommonTimeParameter.getOutputs()) {
            if (!(beastInterface instanceof SuperSpreaderParameterization))
                continue;
            SuperSpreaderParameterization parameterization = (SuperSpreaderParameterization)beastInterface;
            if (!parameterization.SSFracInput.get().equals(skylineScalarWithCommonTimeParameter)) {
                continue;
            }
            for (BEASTInterface beastInterfaceSuper : parameterization.getOutputs()) {
                if (!(beastInterfaceSuper instanceof BirthDeathMigrationDistribution)) {
                    continue;
                }
                BirthDeathMigrationDistribution bdmmDistr = (BirthDeathMigrationDistribution)beastInterfaceSuper;
                TypeSet typeSet = bdmmDistr.parameterizationInput.get().typeSetInput.get();
                typeSet.initAndValidate();
                int nTypes = typeSet.getNTypes();

                RealParameter startTypeProbs = (RealParameter) bdmmDistr.startTypePriorProbsInput.get();
                TraitSet traitSet = bdmmDistr.typeTraitSetInput.get();

                RealParameter valuesParameter = (RealParameter) skylineScalarWithCommonTimeParameter.skylineValuesInput.get();
                Double ssfrac = valuesParameter.getValue();

                startTypeProbs.setDimension(nTypes);
                startTypeProbs.valuesInput.setValue((1.0 - ssfrac) + " " + ssfrac, startTypeProbs);

                try {
                    startTypeProbs.initAndValidate();
                } catch (Exception ex) {
                    System.err.println("Error updating startTypeProbs.");
                }
                try {
                    traitSet.initAndValidate();
                } catch (Exception ex) {
                    System.err.println("Error updating traitSet.");
                }
            }
        }
    }

    @Override
    public void refreshPanel() {
        super.refreshPanel();
        updateFrequenciesForSSFrac();
        sync();
    }

    private String getPartitionID() {
        return skylineScalarWithCommonTimeParameter.getID().split("\\.t:")[1];
    }

    protected Tree getTree() {
        return (Tree) doc.pluginmap.get("Tree.t:" + getPartitionID());
    }


    public abstract static class ValuesTableEntry { }

}
