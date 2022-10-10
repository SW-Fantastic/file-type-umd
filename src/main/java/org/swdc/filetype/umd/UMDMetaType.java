package org.swdc.filetype.umd;

public enum UMDMetaType {

    Title(2),
    Author(3),
    PublishYear(4),
    PublishMonth(5),
    PublishDay(6),
    BookType(7),
    Publisher(8),
    Vendor(9),
    OriginLength(0xb);

    private final int val;
    UMDMetaType(int val) {
        this.val = val;
    }
    public static UMDMetaType cast(long val) {
        for (UMDMetaType type: values()) {
            if (type.val == val) {
                return type;
            }
        }
        return null;
    }

}
