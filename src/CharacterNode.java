// e2ra de 34an tt5ayel wzefet el class da
// mel a5er ba5la2 character gded w bdelo el beta2a bta3to

/*## What each part looks like in memory
After writing:  CRDTCharacter e = new CRDTCharacter("A", 2, 'E', "A", 1);

    e
    │
    ▼
┌─────────────────────────────┐
│ siteId      = "A"           │  ← User A created this
│ clock       = 2             │  ← at logical time 2
│ value       = 'E'           │  ← the letter E
│ afterSiteId = "A"           │  ← lives after character...  ///// assuming enena already katben [H]
│ afterClock  = 1             │  ← ...(A,1) which is 'H'
│ isDeleted   = false         │
│ isBold      = false         │
│ isItalic    = false         │
└─────────────────────────────┘
*/

public class CharacterNode {

    //fields
    public String siteId;      //the user who created the character
    public int clock;          //time it was created

    public char value;         // elvalue nafso

    public String afterSiteId;
    public int afterClock;     //ID of the char before me


    public boolean isDeleted;  // tombstone:law true character mesh hay get printed
    public boolean isBold;
    public boolean isItalic;



    // constructor
    public CharacterNode(String siteId, int clock, char value,
                         String afterSiteId, int afterClock) {

        this.siteId      = siteId;
        this.clock       = clock;
        this.value       = value;
        this.afterSiteId = afterSiteId;  // null if inserting at the very start
        this.afterClock  = afterClock;   // 0 if inserting at the very start
        this.isDeleted   = false;        // brand new, not deleted
        this.isBold      = false;        // no formatting by default
        this.isItalic    = false;
    }


    // ─────────────────────────────────────────
    // METHODS hasta5demha fe testing odam
    // ─────────────────────────────────────────

    // checks if this character has the given id
    public boolean hasSameId(String siteId, int clock) {
        return this.siteId.equals(siteId) && this.clock == clock;
    }

    //3ayez ashoof heya ba3d el id da wala la2
    public boolean isAfter(String siteId, int clock) {
        if (this.afterSiteId == null) return false;
        return this.afterSiteId.equals(siteId) && this.afterClock == clock;
    }

    //34an a print el character bel details bta3to el mohema law e7tagt ---> hatektebo kda [A,3|E]
    @Override
    public String toString() {
        String base = "[" + siteId + "," + clock + "|" + value + "]";
        if (isDeleted) base += "(deleted)";
        if (isBold)    base += "(bold)";
        if (isItalic)  base += "(italic)";
        return base;
    }
}

