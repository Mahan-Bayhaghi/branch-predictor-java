package hardwar.branch.prediction.judged.PAg;

import hardwar.branch.prediction.shared.*;
import hardwar.branch.prediction.shared.devices.*;

import java.util.Arrays;

public class PAg implements BranchPredictor {
    private final ShiftRegister SC; // saturating counter register
    private final RegisterBank PABHR; // per address branch history register
    private final Cache<Bit[], Bit[]> PHT; // page history table

    public PAg() {
        this(4, 2, 8);
    }

    /**
     * Creates a new PAg predictor with the given BHR register size and initializes the PABHR based on
     * the branch instruction size and BHR size
     *
     * @param BHRSize               the size of the BHR register
     * @param SCSize                the size of the register which hold the saturating counter value
     * @param branchInstructionSize the number of bits which is used for saving a branch instruction
     */
    public PAg(int BHRSize, int SCSize, int branchInstructionSize) {
        // TODO: complete the constructor
        // Initialize the PABHR with the given bhr and branch instruction size
        PABHR = new RegisterBank(branchInstructionSize , BHRSize);

        // Initialize the PHT with a size of 2^size and each entry having a saturating counter of size "SCSize"
        PHT = new PageHistoryTable(1<<BHRSize , SCSize);

        // Initialize the SC register
        this.SC = new SIPORegister("shit-name1" , SCSize , null);
    }

    /**
     * @param instruction the branch instruction
     * @return the predicted outcome of the branch instruction (taken or not taken)
     */
    @Override
    public BranchResult predict(BranchInstruction instruction) {
        // TODO : complete Task 1
        ShiftRegister currentBHR = this.PABHR.read(instruction.getInstructionAddress());
        Bit[] bhrValue = currentBHR.read();
        this.PHT.putIfAbsent(bhrValue , getDefaultBlock());
        this.SC.load(this.PHT.get(bhrValue));

        if (this.SC.read()[0].equals(Bit.ZERO))
            return  BranchResult.NOT_TAKEN;
        else return BranchResult.TAKEN;
    }

    /**
     * @param instruction the branch instruction
     * @param actual      the actual result of branch (taken or not)
     */
    @Override
    public void update(BranchInstruction instruction, BranchResult actual) {
        ShiftRegister BHR = PABHR.read(instruction.getInstructionAddress());
        Bit[] new_value;
        if (actual.equals(BranchResult.TAKEN))
            new_value = CombinationalLogic.count(this.SC.read() , true , CountMode.SATURATING);
        else
            new_value = CombinationalLogic.count(this.SC.read() , false, CountMode.SATURATING);
        this.PHT.put(BHR.read() , new_value);

        if (actual.equals(BranchResult.TAKEN))
            BHR.insert(Bit.ONE);
        else
            BHR.insert(Bit.ZERO);
    }

    /**
     * @return a zero series of bits as default value of cache block
     */
    private Bit[] getDefaultBlock() {
        Bit[] defaultBlock = new Bit[SC.getLength()];
        Arrays.fill(defaultBlock, Bit.ZERO);
        return defaultBlock;
    }

    @Override
    public String monitor() {
        return "PAg predictor snapshot: \n" + PABHR.monitor() + SC.monitor() + PHT.monitor();
    }
}
