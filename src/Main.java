// ─────────────────────────────────────────
// da el exhaustive testbench ba test en kol 7aga sha8ala sa7 zai ma elmafrod teshta8al
// d kolo tab3an hardcode
// 34an tefhamooh aktar e3tebro el kol (RemoteInsert/RemoteDelete) (da el server)
// ─────────────────────────────────────────

public class Main {
    public static void main(String[] args) {

        System.out.println("═══ TEST 1: Basic Sequential Insert ═══");
        DocumentCRDT crdtA = new DocumentCRDT("A");
        DocumentCRDT crdtB = new DocumentCRDT("B");
        DocumentCRDT crdtC = new DocumentCRDT("C");

        CharacterNode h  = crdtA.localInsert('H', null, 0);
        CharacterNode e  = crdtA.localInsert('E', h.siteId,  h.clock);
        CharacterNode l1 = crdtA.localInsert('L', e.siteId,  e.clock);
        CharacterNode l2 = crdtA.localInsert('L', l1.siteId, l1.clock);
        CharacterNode o  = crdtA.localInsert('O', l2.siteId, l2.clock);

        for (CharacterNode c : new CharacterNode[]{h, e, l1, l2, o}) {
            crdtB.remoteInsert(c);
            crdtC.remoteInsert(c);
        }
        System.out.println("A: " + crdtA.getVisibleText()); // HELLO
        System.out.println("B: " + crdtB.getVisibleText()); // HELLO
        System.out.println("C: " + crdtC.getVisibleText()); // HELLO


        System.out.println("\n═══ TEST 2: Concurrent Insert Same Position ═══");
        CharacterNode x = crdtA.localInsert('X', e.siteId, e.clock);
        CharacterNode y = crdtB.localInsert('Y', e.siteId, e.clock);
        crdtA.remoteInsert(y);
        crdtB.remoteInsert(x);
        System.out.println("A: " + crdtA.getVisibleText()); // must match B
        System.out.println("B: " + crdtB.getVisibleText()); // must match A


        System.out.println("\n═══ TEST 3: Basic Delete (Tombstone) ═══");
        crdtA.localDelete(e.siteId, e.clock);
        crdtB.remoteDelete(e.siteId, e.clock);
        System.out.println("A: " + crdtA.getVisibleText()); // no E
        System.out.println("B: " + crdtB.getVisibleText()); // no E
        System.out.println("A internal: " + crdtA.getInternalState());


        System.out.println("\n═══ TEST 4: Insert After Deleted Character ═══");
        CharacterNode k = crdtA.localInsert('K', e.siteId, e.clock);
        crdtB.remoteInsert(k);
        System.out.println("A: " + crdtA.getVisibleText());
        System.out.println("B: " + crdtB.getVisibleText()); // must match


        System.out.println("\n═══ TEST 5: Double Delete Same Character ═══");
        crdtA.remoteDelete(e.siteId, e.clock); // already deleted, no crash
        System.out.println("A: " + crdtA.getVisibleText()); // same, no crash


        System.out.println("\n═══ TEST 6: Insert at Very End ═══");
        CharacterNode bang = crdtA.localInsert('!', o.siteId, o.clock);
        crdtB.remoteInsert(bang);
        System.out.println("A: " + crdtA.getVisibleText());
        System.out.println("B: " + crdtB.getVisibleText()); // must match


        System.out.println("\n═══ TEST 7: Insert at Very Beginning ═══");
        CharacterNode z1 = crdtA.localInsert('Z', null, 0);
        CharacterNode z2 = crdtB.localInsert('W', null, 0);
        crdtA.remoteInsert(z2);
        crdtB.remoteInsert(z1);
        System.out.println("A: " + crdtA.getVisibleText()); // must match B
        System.out.println("B: " + crdtB.getVisibleText()); // must match A


        System.out.println("\n═══ TEST 8: Three Users Concurrent Insert ═══");
        CharacterNode p = crdtA.localInsert('P', l1.siteId, l1.clock);
        CharacterNode q = crdtB.localInsert('Q', l1.siteId, l1.clock);
        CharacterNode r = crdtC.localInsert('R', l1.siteId, l1.clock);
        crdtC.remoteInsert(x); crdtC.remoteInsert(y);
        crdtC.remoteInsert(k); crdtC.remoteInsert(bang);
        crdtC.remoteInsert(z1); crdtC.remoteInsert(z2);
        crdtA.remoteInsert(q); crdtA.remoteInsert(r);
        crdtB.remoteInsert(p); crdtB.remoteInsert(r);
        crdtC.remoteInsert(p); crdtC.remoteInsert(q);
        System.out.println("A: " + crdtA.getVisibleText()); // all must match
        System.out.println("B: " + crdtB.getVisibleText());
        System.out.println("C: " + crdtC.getVisibleText());


        System.out.println("\n═══ TEST 9: Out of Order Arrival ═══");
        CharacterNode first  = crdtA.localInsert('F', o.siteId, o.clock);
        CharacterNode second = crdtA.localInsert('G', first.siteId, first.clock);
        crdtB.remoteInsert(second); // arrives before first
        crdtB.remoteInsert(first);  // first arrives late
        System.out.println("A: " + crdtA.getVisibleText());
        System.out.println("B: " + crdtB.getVisibleText()); // must match A


        System.out.println("\n═══ TEST 10: Empty Document ═══");
        DocumentCRDT empty = new DocumentCRDT("A");
        System.out.println("empty text: '" + empty.getVisibleText() + "'");
        System.out.println("empty internal: " + empty.getInternalState());
        empty.remoteDelete("A", 999); // warning, no crash
    }
}
