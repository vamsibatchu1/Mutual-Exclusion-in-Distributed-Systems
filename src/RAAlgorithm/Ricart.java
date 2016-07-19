package RAAlgorithm;

import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

import controllerUnit.Node;
import shareUtil.IMutualExclusiveStrategy;
import shareUtil.IreceiveMessageWithClock;
import shareUtil.MessageReceiveService;
import shareUtil.VectorClockService;

public class Ricart implements IMutualExclusiveStrategy,IreceiveMessageWithClock{
	
	public int[] requestQueue;
	public Node node;
	public LamportClock clock;
	public int requestSentClock=Integer.MAX_VALUE;
	public Status currentStatus;
	private Status entering;
	private Status notEnter;
	BlockingQueue<Integer> pendingQueue;
	private int numOfOk;
	private boolean isReceiving;
	public VectorClockService vclock;
	public CorrectVerification verifaction;
	
	public Ricart(Node node){

		this.node=node;
		this.vclock=VectorClockService.getInstance();
		verifaction=new CorrectVerification("algorithmTest"+node.localInfor.nodeId);
		requestQueue=new int[node.numNodes];
		this.clock=new LamportClock();
		this.entering=new EnteringCS(this);
		this.notEnter=new NotEnter(this);
		currentStatus=notEnter;
		pendingQueue=new PriorityBlockingQueue<>();  //impronve to keep thread safe ,when entering status is adding and notenter is poll
		MessageReceiveService.getInstance().registerWithClock(this);
		
		
	}

	
	public void changeToEntering(){
		this.currentStatus=this.entering;
	}
	public void changeToNotEntering(){
		this.currentStatus=this.notEnter;
	}

	@Override
	
	public synchronized void csEnter() {
		clock.update();//for event of enter cs
		vclock.update();
		this.numOfOk=0;
		changeToEntering();
		this.currentStatus.execute();
		while(this.getNumOfOk()<(this.node.numNodes-1)){
			try {
				this.wait();	//can i do better?
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		verifaction.enterCS(vclock.toString());
	}

	public Queue<Integer> getQueue(){
		return this.pendingQueue;
	}
	
	@Override
	public void csLeave() {
		requestSentClock=Integer.MAX_VALUE;
		verifaction.leaveCS(vclock.toString());
		this.changeToNotEntering();
		//need to determine when the last message put int queue.
		this.currentStatus.execute();
	}
	
	public boolean isReceiving() {
		return isReceiving;
	}


	public synchronized void setReceiving(boolean isReceiving) {
		this.isReceiving = isReceiving;
	}


	

	@Override
	public void receive(String message, int channel, long milliseconds) {
		Message receivedMsg = MessageFactory.getSingleton().parseMessage(message);
		this.currentStatus.receive(receivedMsg, channel, milliseconds);
	
		
	}




	public int getNumOfOk() {
		return numOfOk;
	}




	public synchronized void addNumOfOk() {
		this.numOfOk ++;
	}



	

}