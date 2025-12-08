package bdmmprime.beauti;

import bdmmprime.parameterization.SkylineParameter;
import bdmmprime.parameterization.SkylineScalarCommonTimeParameter;
import beast.base.core.BEASTInterface;
import beast.base.core.Function;
import beast.base.core.Input;
import beast.base.inference.parameter.RealParameter;
import beastfx.app.inputeditor.BeautiDoc;
import beastfx.app.util.FXUtils;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValueBase;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import javafx.util.converter.DoubleStringConverter;

public class SkylineScalarCommonTimeInputEditor extends SkylineInputEditor {

    SkylineScalarCommonTimeParameter skylineScalarCommonTimeParameter;

    public SkylineScalarCommonTimeInputEditor(BeautiDoc doc) {
        super(doc);
    }

    @Override
    public Class<?> type() {
        return SkylineScalarCommonTimeParameter.class;
    }

    @Override
    public void init(Input<?> input, BEASTInterface beastObject, int itemNr,
                     ExpandOption isExpandOption, boolean addButtons) {

        skylineParameter = (SkylineParameter) input.get();
        skylineScalarCommonTimeParameter = (SkylineScalarCommonTimeParameter) input.get();

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

        RealParameter valuesParameter = (RealParameter) skylineScalarCommonTimeParameter.skylineValuesInput.get();

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
            skylineScalarCommonTimeParameter.linkIdenticalValuesInput.setValue(newValue, beastObject);
            sync();
        });

        skylineScalarCommonTimeParameter.initAndValidate();

        updateValuesUI();

        valuesTable.setFixedCellSize(25);
        valuesTable.prefHeightProperty().bind(valuesTable.fixedCellSizeProperty()
                .multiply(Bindings.size(valuesTable.getItems()).add(1.1)));

    }

    @Override
    void ensureValuesConsistency() {

        Input<Function> changeTimesInput;
        changeTimesInput = skylineScalarCommonTimeParameter.commonSkylineInput.get().changeTimesInput;


        int nEpochs = changeTimesInput.get() == null
                ? 1
                : changeTimesInput.get().getDimension() + 1;

        RealParameter valuesParam = (RealParameter) skylineScalarCommonTimeParameter.skylineValuesInput.get();

        System.out.println("Number of epochs: " + nEpochs);

        valuesParam.setDimension(nEpochs);

        if (changeTimesInput.get() != null)
            ((RealParameter) changeTimesInput.get()).initAndValidate();
        sanitiseRealParameter(valuesParam);
        skylineScalarCommonTimeParameter.initAndValidate();
    }


    @Override
    void updateValuesUI() {
        valuesTable.getColumns().clear();
        valuesTable.getItems().clear();

        int nChanges = skylineScalarCommonTimeParameter.getChangeCount();

        RealParameter valuesParameter = (RealParameter) skylineScalarCommonTimeParameter.skylineValuesInput.get();
        TableColumn<ValuesTableEntry, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(p -> new ObservableValueBase<>() {
            @Override
            public String getValue() {
                int type = ((VectorValuesEntry) p.getValue()).type;
                return type < 0
                        ? "ALL"
                        : skylineScalarCommonTimeParameter.typeSetInput.get().getTypeName(type);
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
}
