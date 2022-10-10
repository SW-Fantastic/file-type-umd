package org.swdc.filetype.umd;

public enum UMDType {

    Text(1),
    Magazine(2);

    private int val;
    UMDType(int val) {
        this.val = val;
    }

    public int getVal() {
        return val;
    }

    public static UMDType cast(int val) {
        switch (val) {
            case 1:
                return Text;
            case 2:
                return Magazine;
        }
        return null;
    }
}
