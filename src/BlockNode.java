public class BlockNode
{
    public String siteId;  /// SITE ID DAH ESM EL USER EL 3AMAL EL BLOCK DAH//
public int clock;

    public String afterSiteId;
    public int afterClock;

    public boolean isDeleted;

    public DocumentCRDT content; //// HENA EL FAR2 3AN CHARACTERNODE, HENAK ANA BADDY VALUE L KUL CHARACTER HENA HWA FULL LINE OF CHARACTERS

public BlockNode(String siteId, int clock, String afterSiteId, int afterClock)
{
    this.siteId      = siteId;
    this.clock       = clock;
    this.afterSiteId = afterSiteId;
    this.afterClock  = afterClock;
    this.isDeleted   = false;
    this.content     = new DocumentCRDT(siteId); // empty line, ready to be typed in
}

    public boolean hasSameId(String siteId,int clock)         /// 3ayz ASHOF EL BLOCK DAH MOWGOD WLA LA BEL ID WEL TIME BTA3O F FOR EXAMPLE HAKTB { if(block.hasSameId(A,3)) }
    {
        return this.siteId.equals(siteId) && this.clock == clock; //// EQUALS DEH LL STRING ONLY//
    }

    @Override
    public String toString()
    {
        String base = "[Block " + siteId + "," + clock + " | " + content.getVisibleText() + "]";  ///////// [Block A,1 | Hello World]///// DAH EL MFROD YETL3 MENHA
        if (isDeleted) base += "(deleted)";    ///////// LW HWA DELETED ALREADY BA7OTT GAMBEH TEXT DELETED////
        return base;
    }
}



