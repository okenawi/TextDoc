import java.util.LinkedList;

public class DocumentCRDT  {

    //FIELDS
    private LinkedList<CharacterNode> characters;//List el feha kol el characters bta3et eldocument da
    //metrateben be tarteeb el doc

    private String siteId; //da y2olak document da belong for which user [A,B,C,D....]


    private int clock;//elclock mawgoda hena 34an de wazefet el document mesh el characters
    //elclock btashta8al enaha betzeed kol mara el user BTA3HA creates new character
    //betzeed bas 3omraha ma t2el



    // CONSTRUCTOR
    public DocumentCRDT (String siteId) {
        this.siteId     = siteId;
        this.clock      = 0;
        this.characters = new LinkedList<>();
    }

    // ─────────────────────────────────────────
    // LOCAL INSERT
    //zai ma etfa2t m3ako kol user haykteb 7arf hayb2a inserted in HIS document 3alatol
    //de b2a el function el bte3mel kda
    //ka2enak katabt bel keyboard
    // ─────────────────────────────────────────
    public CharacterNode localInsert(char value, String afterSiteId, int afterClock) {

        // step 1: zai ma olna clock tzeed 3alatol talama fe character gded
        this.clock++;

        // step 2: create the new character
        // da constructor el character node boso 3aleha henak
        CharacterNode newChar = new CharacterNode(
                this.siteId,    // who created it
                this.clock,     // when
                value,          // the letter
                afterSiteId,    // comes after this id...
                afterClock      // ...with this clock
        );

        // step 3: insert it into our own list (el tasgeel fel lista dayman through remoteinsert)
        // we reuse remoteInsert because the insertion logic is identical
        // whether the character came from us or from someone else
        // law mesh fahem leeh: lazem tre2et el tasgel tkon mowahada sawa2 ana el kateb aw gayali mn 7ad mn bara!!!
        remoteInsert(newChar);

        // step 4: return it so it can be sent over the network to other users
        return newChar;
    }


    // ─────────────────────────────────────────
    // LOCAL DELETE
    // called when THIS user deletes a character
    //lama el user yedos delete in HIS document
    // ─────────────────────────────────────────
    public CharacterNode localDelete(String siteId, int clock) {

        // find and tombstone it
        // we reuse remoteDelete because the logic is identical
        remoteDelete(siteId, clock);

        // return the character so the delete op can be sent over the network
        // the other users need to know WHICH character to delete (by id)
        return characters.get(findIndexById(siteId, clock));
    }


    // ─────────────────────────────────────────
    // REMOTE INSERT
    // called when we receive an insert op from another user
    // also called internally by localInsert
    // ─────────────────────────────────────────
    public void remoteInsert(CharacterNode incoming) {

        // update our clock to stay in sync with the network
        // we always take the maximum of what we know
        this.clock = Math.max(this.clock, incoming.clock);

        //el gai da data structures implementation linked list law fakren 3ady gedan

        // ── SPECIAL CASE: insert at the very beginning ──
        // afterSiteId == null means "insert before everything"
        if (incoming.afterSiteId == null) {

            // there might already be other characters at position 0
            // we need to check tiebreaker against them
            int insertAt = 0;//nemsek awel wa7da no3od nakrenhom bel incoming l7ad ma ala2y makanha
            while (insertAt < characters.size() &&
                    characters.get(insertAt).afterSiteId == null &&
                    winsOver(characters.get(insertAt), incoming)) {
                insertAt++;
            }
            characters.add(insertAt, incoming);
            return;
        }

        // ── NORMAL CASE: find the afterId position ──

        // step 1: find the index of the character we come after
        int afterIndex = findIndexById(incoming.afterSiteId, incoming.afterClock);

        // step 2: if not found, the character we depend on hasn't arrived yet
        // this is the out-of-order problem we discussed
        // for phase 1 we assume ops arrive in order
        // in phase 2/3 you would add a waiting queue here
        if (afterIndex == -1) {
            System.out.println("WARNING: afterId not found, op may be out of order");
            return;
        }

        // step 3: find the correct insertion point AFTER afterIndex
        // we scan forward past any characters that beat us in the tiebreaker
        int insertAt = afterIndex + 1;

        while (insertAt < characters.size()) {

            CharacterNode current = characters.get(insertAt);

            // stop if this character points to a DIFFERENT afterId
            // meaning it belongs to a different part of the document
            if (!current.afterSiteId.equals(incoming.afterSiteId) ||
                    current.afterClock != incoming.afterClock) {
                break;
            }

            // both current and incoming have the same afterId
            // this is a conflict → run tiebreaker
            if (winsOver(current, incoming)) {
                // current beats incoming → skip past current
                insertAt++;
            } else {
                // incoming beats current → insert here
                break;
            }
        }

        // step 4: insert at the found position
        characters.add(insertAt, incoming);
    }


    // ─────────────────────────────────────────
    // REMOTE DELETE
    // called when we receive a delete op from another user
    // also called internally by localDelete
    // ─────────────────────────────────────────
    public void remoteDelete(String siteId, int clock) {

        int index = findIndexById(siteId, clock);

        if (index == -1) {
            // character not found
            // could be out of order arrival, ignore for phase 1
            System.out.println("WARNING: character to delete not found");
            return;
        }

        // tombstone it, do NOT remove it from the list
        characters.get(index).isDeleted = true;
    }


    // ─────────────────────────────────────────
    // FIND INDEX BY ID
    // scans the list and returns the index of the
    // character with the given (siteId, clock)
    // returns -1 if not found
    // ─────────────────────────────────────────
    private int findIndexById(String siteId, int clock) {
        for (int i = 0; i < characters.size(); i++) {
            if (characters.get(i).hasSameId(siteId, clock)) {
                return i;
            }
        }
        return -1;//mynf34 maslan a return zero 34an da mmkn ykon index item mawgod aslun !!
    }


    // ─────────────────────────────────────────
    // WINS OVER (tiebreaker)
    // when two characters have the same afterId,
    // returns true if 'a' should come BEFORE 'b'
    // must be deterministic: same answer on every machine
    // ─────────────────────────────────────────
    private boolean winsOver(CharacterNode a, CharacterNode b) {

        // first compare siteIds alphabetically
        // "B" > "A" → B wins → B goes first
        int siteComparison = a.siteId.compareTo(b.siteId);
        if (siteComparison != 0) {
            return siteComparison > 0;
            // positive means a's siteId is greater → a wins
        }

        // if same siteId (same user, which shouldn't happen in conflict
        // but just in case), compare clocks
        // higher clock wins
        return a.clock > b.clock;
    }


    // ─────────────────────────────────────────
    // GET VISIBLE TEXT
    // returns only non-deleted characters as a string
    // this is what gets displayed in the UI
    // ─────────────────────────────────────────
    public String getVisibleText() {
        StringBuilder sb = new StringBuilder();
        for (CharacterNode c : characters) {
            if (!c.isDeleted) {
                sb.append(c.value);
            }
        }
        return sb.toString();
    }


    // ─────────────────────────────────────────
    // GET INTERNAL STATE
    // shows the full list including tombstones
    // only for debugging
    // ─────────────────────────────────────────
    public String getInternalState() {
        StringBuilder sb = new StringBuilder();
        for (CharacterNode c : characters) {
            sb.append(c.toString()).append(" → ");
        }
        sb.append("END");
        return sb.toString();
    }


    // ─────────────────────────────────────────
    // APPLY FORMATTING
    // toggles bold or italic on a character by id
    // ─────────────────────────────────────────
    public void applyBold(String siteId, int clock, boolean value) {
        int index = findIndexById(siteId, clock);
        if (index != -1) {
            characters.get(index).isBold = value;
        }
    }

    public void applyItalic(String siteId, int clock, boolean value) {
        int index = findIndexById(siteId, clock);
        if (index != -1) {
            characters.get(index).isItalic = value;
        }
    }
}