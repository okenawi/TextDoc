public class CharacterNode {


    public String siteId;
    public int clock;

    public char value;

    public String afterSiteId;
    public int afterClock;


    public boolean isDeleted;
    public boolean isBold;
    public boolean isItalic;



    // constructor
    public CharacterNode(String siteId, int clock, char value,
                         String afterSiteId, int afterClock) {

        this.siteId      = siteId;
        this.clock       = clock;
        this.value       = value;
        this.afterSiteId = afterSiteId;
        this.afterClock  = afterClock;
        this.isDeleted   = false;
        this.isBold      = false;
        this.isItalic    = false;
    }



    public boolean hasSameId(String siteId, int clock) {
        return this.siteId.equals(siteId) && this.clock == clock;
    }

    public boolean isAfter(String siteId, int clock) {
        if (this.afterSiteId == null) return false;
        return this.afterSiteId.equals(siteId) && this.afterClock == clock;
    }

    @Override
    public String toString() {
        String base = "[" + siteId + "," + clock + "|" + value + "]";
        if (isDeleted) base += "(deleted)";
        if (isBold)    base += "(bold)";
        if (isItalic)  base += "(italic)";
        return base;
    }
}

