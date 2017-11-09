package com.serotonin.json.junit.vo;

import java.util.List;
import java.util.Map;

public class Compound {
    private Primitives primitives = new Primitives();
    private List<String> list1;
    private List<? extends BaseClass> list2;
    private Map<?, ?> map;
    private ImSerializable imSerializable = new ImSerializable();

    public Primitives getPrimitives() {
        return primitives;
    }

    public void setPrimitives(Primitives primitives) {
        this.primitives = primitives;
    }

    public List<String> getList1() {
        return list1;
    }

    public void setList1(List<String> list1) {
        this.list1 = list1;
    }

    public List<? extends BaseClass> getList2() {
        return list2;
    }

    public void setList2(List<? extends BaseClass> list2) {
        this.list2 = list2;
    }

    public Map<?, ?> getMap() {
        return map;
    }

    public void setMap(Map<?, ?> map) {
        this.map = map;
    }

    public ImSerializable getImSerializable() {
        return imSerializable;
    }

    public void setImSerializable(ImSerializable imSerializable) {
        this.imSerializable = imSerializable;
    }
}
