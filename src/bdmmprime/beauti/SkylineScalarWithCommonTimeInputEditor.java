package bdmmprime.beauti;

import bdmmprime.distribution.BirthDeathMigrationDistribution;
import bdmmprime.parameterization.SkylineParameter;
import bdmmprime.parameterization.SkylineScalarWithCommonTimeParameter;
import bdmmprime.parameterization.SuperSpreaderParameterization;
import bdmmprime.parameterization.TypeSet;
import beast.base.core.BEASTInterface;
import beast.base.core.Function;
import beast.base.core.Input;
import beast.base.evolution.tree.TraitSet;
import beast.base.inference.parameter.RealParameter;
import beastfx.app.inputeditor.BeautiDoc;
import beastfx.app.util.FXUtils;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValueBase;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.converter.DoubleStringConverter;

public class SkylineScalarWithCommonTimeInputEditor extends SkylineInputEditor {

    SkylineScalarWithCommonTimeParameter skylineScalarWithCommonTimeParameter;

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

        skylineParameter = (SkylineParameter) input.get();
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
        linkValuesCheckBox.setSelected(skylineParameter.linkIdenticalValuesInput.get());
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

    @Override
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


    @Override
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
     * Ugly hack to keep update the type trait set panel and the equilibrium frequencies..
     */
    void updateFrequenciesForSSFrac() {

        for (BEASTInterface beastInterface : skylineParameter.getOutputs()) {
            System.out.println("JKE checking class of beastInterface = " + beastInterface);
            if (!(beastInterface instanceof SuperSpreaderParameterization parameterization))
                continue;

            if (!parameterization.SSFracInput.get().equals(skylineParameter)) {
                System.out.println("JKE not happy with the parameterizationInput = " + parameterization.SSFracInput.get());
                continue;
            }

            for (BEASTInterface beastInterfaceSuper : parameterization.getOutputs()) {
                System.out.println("JKE 1");
                if (!(beastInterfaceSuper instanceof BirthDeathMigrationDistribution bdmmDistr)) {
                    System.out.println("JKE 2");
                    continue;
                }
                System.out.println("JKE 3");
                TypeSet typeSet = bdmmDistr.parameterizationInput.get().typeSetInput.get();
                typeSet.initAndValidate();
                int nTypes = typeSet.getNTypes();
                System.out.println("JKE nTypes = " + nTypes);

                RealParameter startTypeProbs = (RealParameter) bdmmDistr.startTypePriorProbsInput.get();
                TraitSet traitSet = bdmmDistr.typeTraitSetInput.get();

                RealParameter valuesParameter = (RealParameter) skylineScalarWithCommonTimeParameter.skylineValuesInput.get();
                Double ssfrac = valuesParameter.getValue();

                startTypeProbs.setDimension(nTypes);
                startTypeProbs.valuesInput.setValue((1.0 - ssfrac) + " " + ssfrac, startTypeProbs);
                System.out.println("JKE startTypeProbs.valuesInput.value = " + startTypeProbs.valuesInput.get());

                try {
                    startTypeProbs.initAndValidate();
                    traitSet.initAndValidate();
                    bdmmDistr.initAndValidate();
                } catch (Exception ex) {
                    System.err.println("Error updating start type probabilities.");
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

}
