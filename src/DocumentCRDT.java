import java.util.LinkedList;

public class DocumentCRDT  {

    private LinkedList<CharacterNode> characters;

    private String siteId;


    private int clock;



    public DocumentCRDT (String siteId) {
        this.siteId     = siteId;
        this.clock      = 0;
        this.characters = new LinkedList<>();
    }


    public CharacterNode localInsert(char value, String afterSiteId, int afterClock) {

        this.clock++;


        CharacterNode newChar = new CharacterNode(
                this.siteId,
                this.clock,
                value,
                afterSiteId,
                afterClock
        );


        remoteInsert(newChar);

        return newChar;
    }



    public CharacterNode localDelete(String siteId, int clock) {


        remoteDelete(siteId, clock);


        return characters.get(findIndexById(siteId, clock));
    }



    public void remoteInsert(CharacterNode incoming) {


        this.clock = Math.max(this.clock, incoming.clock);



        if (incoming.afterSiteId == null) {


            int insertAt = 0;
            while (insertAt < characters.size() &&
                    characters.get(insertAt).afterSiteId == null &&
                    winsOver(characters.get(insertAt), incoming)) {
                insertAt++;
            }
            characters.add(insertAt, incoming);
            return;
        }


        int afterIndex = findIndexById(incoming.afterSiteId, incoming.afterClock);


        if (afterIndex == -1) {
            System.out.println("WARNING: afterId not found, op may be out of order");
            return;
        }


        int insertAt = afterIndex + 1;

        while (insertAt < characters.size()) {

            CharacterNode current = characters.get(insertAt);


            if (!java.util.Objects.equals(current.afterSiteId, incoming.afterSiteId) ||
                    current.afterClock != incoming.afterClock) {
                break;
            }


            if (winsOver(current, incoming)) {
                insertAt++;
            } else {
                break;
            }
        }

        characters.add(insertAt, incoming);
    }




    public void remoteDelete(String siteId, int clock) {

        int index = findIndexById(siteId, clock);

        if (index == -1) {

            System.out.println("WARNING: character to delete not found");
            return;
        }

        characters.get(index).isDeleted = true;
    }



    private int findIndexById(String siteId, int clock) {
        for (int i = 0; i < characters.size(); i++) {
            if (characters.get(i).hasSameId(siteId, clock)) {
                return i;
            }
        }
        return -1;
    }



    private boolean winsOver(CharacterNode a, CharacterNode b) {


        int siteComparison = a.siteId.compareTo(b.siteId);
        if (siteComparison != 0) {
            return siteComparison > 0;
        }


        return a.clock > b.clock;
    }





    public String getVisibleText() {
        StringBuilder sb = new StringBuilder();
        for (CharacterNode c : characters) {
            if (!c.isDeleted) {
                sb.append(c.value);
            }
        }
        return sb.toString();
    }




    public CharacterNode getVisibleNodeAt(int visibleIndex) {
        if (visibleIndex < 0) return null;

        int count = 0;
        for (CharacterNode c : characters) {
            if (!c.isDeleted) {
                if (count == visibleIndex) return c;
                count++;
            }
        }
        return null;
    }



    public String getInternalState() {
        StringBuilder sb = new StringBuilder();
        for (CharacterNode c : characters) {
            sb.append(c.toString()).append(" → ");
        }
        sb.append("END");
        return sb.toString();
    }



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


    public int getVisibleIndex(String siteId, int clock) {
        int visibleCount = 0;
        for (CharacterNode c : characters) {
            if (c.hasSameId(siteId, clock)) {
                return visibleCount;
            }
            if (!c.isDeleted) {
                visibleCount++;
            }
        }
        return -1;
    }


    public java.util.List<CharacterNode> getVisibleNodes() {
        java.util.List<CharacterNode> visibleNodes = new java.util.ArrayList<>();
        for (CharacterNode c : characters) {
            if (!c.isDeleted) {
                visibleNodes.add(c);
            }
        }
        return visibleNodes;
    }

}

