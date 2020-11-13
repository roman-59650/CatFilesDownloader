package fr.ulille.phlam.cat;

import javafx.beans.property.*;

public class CatTableEntry {
    private Property<Integer> molTag;
    private Property<String> molName;
    private Property<Boolean> isSelected;

    public CatTableEntry(int tag, String name, boolean sel){
        molTag = new SimpleObjectProperty<>(tag);
        molName = new SimpleStringProperty(name);
        isSelected = new SimpleBooleanProperty(sel);
    }

    public Property<Integer> molTagProperty() {
        return molTag;
    }

    public Property<String> molNameProperty() {
        return molName;
    }

    public Property<Boolean> isSelectedProperty() {
        return isSelected;
    }

    public Boolean isSelected() {
        return isSelected.getValue();
    }

    public Integer getOrigin(){
        return (molTag.getValue()/100)%10;
    }
}
