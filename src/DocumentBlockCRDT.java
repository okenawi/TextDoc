/*DocumentBlockCRDT          ← manages the list of blocks
    │
    ├── BlockNode 1        ← a line
    │       └── DocumentCRDT  ← manages characters inside line 1
    │
    ├── BlockNode 2        ← another line
    │       └── DocumentCRDT  ← manages characters inside line 2  //////////////// BLOCK 3OBARA 3N LINE EL LINE DAH CHARACHTERSSSSS////
    │
    └── BlockNode 3        ← another line
            └── DocumentCRDT  ← manages characters inside line 3
*/
import java.util.LinkedList;

public class DocumentBlockCRDT {

    private LinkedList<BlockNode> blocks; /// LIST OF BLOCKKKSSS//
private String siteId;
    private int clock;




    public DocumentBlockCRDT(String ownerSiteId)
    {
        this.siteId=ownerSiteId;
        this.clock=0;
        this.blocks=new LinkedList<>();  /// HWA SYNTX 8REB BAS M3NAH BY3MLY LINKED LIST GDEDA EMPTY//
        this.clock ++;
        BlockNode firstBlock = new BlockNode(this.siteId,this.clock, null, 0);   /// awl haga 7etta el null WEL ZERO 3SHAN DAH AWL BLOCK MFESH HAGA ABLO//
        blocks.add(firstBlock);     // FUNCTION ADD DEH BT7OT EL HAGAT GWAH EL LIST W NTA BTDEHA BAS EL ROOT EL HWA HENA firstBlock  ///
    }




    //////// LOCAL INSERT////

    public BlockNode LocalInsert(String afterSiteId, int afterClock)  ///// MEL AKHR EL FUNCTION DEH EN LAMA EL USER YDOS ENTER F H3ML LINE GDED F H3ML BLOCK GDEDD///
    {                                                                 /// KHALY BALK EN LOCAL INSERT EL WA7EDA EL MSH VOID 3SHAN HEYA  BTRG3LYY AWL BLOCK///
        this.clock++;
        BlockNode newBlock= new BlockNode(siteId,this.clock,afterSiteId,afterClock);  // TYBB BLOCK WAKHD EL SITEID BTA3 EL DOCUMENT 3SHAN LOCAL INSERT DEH BTKTB FEL DOCUMENT BTA3K NTAA F NTA MSLN BTKTB F DOCUMENT BTA3 USER A YEB2A EL BLOCK DAH ESMO A//
        remoteInsert(newBlock);                                                    ////    8ER B2A REMOTE INSERT MUMMKN AB2A BKTB BLOCK A BAS F DOCUMENT HAD TANY 3ADY
        return newBlock;
    }


    ///// LOCAL DELETE////
    public void LocalDelete(String siteId,int clock )  /// FEL MAIN H2OLO MSLNLOCALDELETE('A',3)//
    {
        remoteDelete(siteId,clock);
    }



    ///// REMOTE DELETE///
    public void remoteDelete(String siteId, int clock) {
        int index = findIndexById(siteId, clock);
        if (index == -1) {
            System.out.println("WARNING: block to delete not found");
            return;
        }
        blocks.get(index).isDeleted = true;
    }




    /////////  REMOOOTEEE INSERT////
    public void remoteInsert(BlockNode incoming) {
        this.clock = Math.max(this.clock, incoming.clock);  /// bzbbbttt el clock m3 b2et el userrss el fel network///
        // insert at the very beginning
        if (incoming.afterSiteId == null)
        {                                                              // m7tagen n3rf syntx kazza haga, 1- block.get(hena get btakhod position)--> ya3ny 3and position kazz hatly el block el henak dah
            //// winsover(blocks.get(positon),incoming"incoming dah block bardo)---> deh boolean btrg33 true lw awl wahed yksb tany wahed...
            int insertAt = 0;                                           /// hwear el after dah 3shan ana btl8bt feh--> 3andy etnen blocks Block(A,1), Block(B,2)
        /// KEDA BLOCK b 3ando aftersiteId 'A' and afterClock'1' ya3ny hwa gy b3d A and time 1
            while (
                    insertAt < blocks.size() &&
                            blocks.get(insertAt).afterSiteId == null &&
                            winsOver(blocks.get(insertAt), incoming)
            )
            {
                insertAt++;
            }
            blocks.add(insertAt, incoming);
            return;
        }

        int afterIndex = findIndexById(incoming.afterSiteId, incoming.afterClock);   //// deh 3amlha t7t ////
        if (afterIndex == -1)
        {
            System.out.println("WARNING: afterId not found for block");
            return;
        }

        int insertAt = afterIndex + 1;
        while (insertAt < blocks.size())
        {
            BlockNode current = blocks.get(insertAt);

            if (current.afterSiteId == null ||
                    !current.afterSiteId.equals(incoming.afterSiteId) ||
                    current.afterClock != incoming.afterClock) {
                break;
            }

            if (winsOver(current, incoming)) {
                insertAt++;
            } else {
                break;
            }
        }
        blocks.add(insertAt, incoming);
    }


    //// FUNCTION BT3ML 7WEAR EL TIE BREAKER///
    private boolean winsOver(BlockNode a, BlockNode b) {
        int siteComparison = a.siteId.compareTo(b.siteId);
        if (siteComparison != 0) {
            return siteComparison > 0;
        }
        return a.clock > b.clock;
    }


    //////// DEH BTGBLY  index elBLOCK EL ANA 3AYZO BEL ID WEL CLOCK////
    private int findIndexById(String siteId, int clock) {
        for (int i = 0; i < blocks.size(); i++) {
            if (blocks.get(i).hasSameId(siteId, clock)) {
                return i;
            }
        }
        return -1;
    }



    /////// DEH EL FUNCTION EL BTTTTB33 EL ET3MLTO REMOTE INSERT MN 8ERHA MSH HSOHOF HAGA 3ALA EL SHASHAA//
    /// BTTTBBTTT333 EL HAAGT EL VISIBLE ONLLYYYYY/// EL MESH DELETEDDD
    public String getVisibleText() {
        String result = "";
        for (BlockNode b : blocks)
        {
            if (!b.isDeleted)
            {
                result = result + b.content.getVisibleText() + "\n";
            }
        }
        return result;
    }

    public BlockNode getBlock(String siteId, int clock) {
        int index = findIndexById(siteId, clock);
        if (index == -1) return null;
        return blocks.get(index);
    }


}




