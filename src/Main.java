public class Main
{
    public static void main(String[] args)
    {

        System.out.println("═══ TEST 1: New Document Has One Empty Block ═══");
        DocumentBlockCRDT docA = new DocumentBlockCRDT("A");
        DocumentBlockCRDT docB = new DocumentBlockCRDT("B");
        docB.LocalDelete("B", 1);  // B is joining, remove auto-created first block
        System.out.println("A: '" + docA.getVisibleText() + "'"); // one empty line


        System.out.println("\n═══ TEST 2: Type Text Into First Block ═══");
        BlockNode firstBlockA = docA.getBlock("A", 1);
        CharacterNode h  = firstBlockA.content.localInsert('H', null, 0);
        CharacterNode e  = firstBlockA.content.localInsert('E', h.siteId,  h.clock);
        CharacterNode l1 = firstBlockA.content.localInsert('L', e.siteId,  e.clock);
        CharacterNode l2 = firstBlockA.content.localInsert('L', l1.siteId, l1.clock);
        CharacterNode o  = firstBlockA.content.localInsert('O', l2.siteId, l2.clock);
        System.out.println("A: " + docA.getVisibleText()); // HELLO


        System.out.println("\n═══ TEST 3: Press Enter - Create New Block ═══");
        BlockNode secondBlockA = docA.LocalInsert("A", 1);
        CharacterNode w  = secondBlockA.content.localInsert('W', null, 0);
        CharacterNode or = secondBlockA.content.localInsert('O', w.siteId,  w.clock);
        CharacterNode r  = secondBlockA.content.localInsert('R', or.siteId, or.clock);
        CharacterNode ld = secondBlockA.content.localInsert('L', r.siteId,  r.clock);
        CharacterNode d  = secondBlockA.content.localInsert('D', ld.siteId, ld.clock);
        System.out.println("A: " + docA.getVisibleText()); // HELLO\nWORLD


        System.out.println("\n═══ TEST 4: Sync Blocks to User B ═══");
        docB.remoteInsert(firstBlockA);
        docB.remoteInsert(secondBlockA);
        System.out.println("A: " + docA.getVisibleText()); // HELLO\nWORLD
        System.out.println("B: " + docB.getVisibleText()); // must match A


        System.out.println("\n═══ TEST 5: Concurrent Block Insert Same Position ═══");
        BlockNode blockFromA = docA.LocalInsert(firstBlockA.siteId, firstBlockA.clock);
        BlockNode blockFromB = docB.LocalInsert(firstBlockA.siteId, firstBlockA.clock);
        blockFromA.content.localInsert('X', null, 0);
        blockFromB.content.localInsert('Y', null, 0);
        docA.remoteInsert(blockFromB);
        docB.remoteInsert(blockFromA);
        System.out.println("A: " + docA.getVisibleText()); // must match B
        System.out.println("B: " + docB.getVisibleText()); // must match A


        System.out.println("\n═══ TEST 6: Delete a Block ═══");
        docA.LocalDelete(secondBlockA.siteId, secondBlockA.clock);
        docB.remoteDelete(secondBlockA.siteId, secondBlockA.clock);
        System.out.println("A: " + docA.getVisibleText()); // no WORLD
        System.out.println("B: " + docB.getVisibleText()); // must match A


        System.out.println("\n═══ TEST 7: Double Delete Same Block ═══");
        docA.remoteDelete(secondBlockA.siteId, secondBlockA.clock); // already deleted, no crash
        System.out.println("A: " + docA.getVisibleText()); // same, no crash



        System.out.println("\n═══ TEST 8: Three Users Concurrent Block Insert ═══");
        DocumentBlockCRDT docC = new DocumentBlockCRDT("C");
        docC.LocalDelete("C", 1);  // C is joining, remove auto-created first block

        // give C the base document first
        docC.remoteInsert(firstBlockA);
        docC.remoteInsert(secondBlockA);

        // give C the concurrent blocks from A and B BEFORE C inserts its own
        docC.remoteInsert(blockFromA);
        docC.remoteInsert(blockFromB);

        // NOW C inserts its own block
        BlockNode blockFromC = docC.LocalInsert(firstBlockA.siteId, firstBlockA.clock);
        blockFromC.content.localInsert('Z', null, 0);

        // sync C's block to A and B
        docA.remoteInsert(blockFromC);
        docB.remoteInsert(blockFromC);

        System.out.println("A: " + docA.getVisibleText()); // all must match
        System.out.println("B: " + docB.getVisibleText());
        System.out.println("C: " + docC.getVisibleText());

    }
}