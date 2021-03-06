package lamportAlgorithm;

import java.util.Comparator;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;

import shareUtil.IMutualExclusiveStrategy;
import shareUtil.IreceiveMessage;
import shareUtil.LamportLogicalClockService;
import shareUtil.MessageReceiveService;
import shareUtil.MessageSenderService;

public class LamportAlgorithm implements IMutualExclusiveStrategy,IreceiveMessage {
	//LamportLogicalClockService clock ;
	Set<Integer> conditionL1 = new HashSet<Integer>();
	PriorityBlockingQueue <TimeStampWithID> pqueue ;
	TimeStampWithID localRequestStamp; 
    int localId ;
    int numOfNode;
    IreceiveMessage application = null;    
	public LamportAlgorithm(int numOfNode, int localId/*, IreceiveMessage application*/){
		Comparator<TimeStampWithID> comparator = new TimeStampWithID(0,0);
		pqueue = new PriorityBlockingQueue<TimeStampWithID>(numOfNode, comparator);
		localRequestStamp = new TimeStampWithID(localId, Integer.MAX_VALUE);
		//clock = LamportLogicalClockService.getInstance();
		this.localId = localId;
		this.numOfNode = numOfNode;
		MessageReceiveService.getInstance().register(this);
		//this.application = application;
	}
	
	public void receive(String message,int channel){
		Message msg = MessageParser.getSingleton().parser(message);		
		//clock.receiveAction(msg.getTimpeStamp());
		String type = msg.getType();
		if(type==null){
			//application.receive(message, channel);
			return;
		}
		int rtimestamp = msg.getTimpeStamp();		
		TimeStampWithID receivedTimeStamp = new TimeStampWithID(channel, rtimestamp);
		// be careful of the order of if statement.
		if(!conditionL1.contains(receivedTimeStamp.nodeId)
				&& localRequestStamp.compare(receivedTimeStamp, localRequestStamp)==1)
		{
			conditionL1.add(receivedTimeStamp.nodeId);
		}
		
		if(type.equals(MessageFactory.getSingleton().typeRequest)){
			handlerRequest((MessageRequest) msg, channel);
		}else if(type.equals(MessageFactory.getSingleton().typeRelease)){
			handlerRelease((MessageRelease) msg, channel);
		}else if(type.equals(MessageFactory.getSingleton().typeReply)){
			handlerReply((MessageReply) msg , channel);
		}else{
			// deliver message to application
			//application.receive(message, channel);
		}	
		
		if((!pqueue.isEmpty())&&pqueue.peek().nodeId == localId && conditionL1.size()== numOfNode-1){
			synchronized(this){
				this.notifyAll();
			}			
		}
	}
	
	public void handlerRequest(MessageRequest msg, int channel){
		int rtimestamp = msg.getTimpeStamp();		
		TimeStampWithID receivedTimeStamp = new TimeStampWithID(channel, rtimestamp);
		pqueue.add(receivedTimeStamp);
		String type = MessageFactory.getSingleton().typeReply;
		Message reply = preSentMessage(type);
		MessageSenderService.getInstance().send(reply.toString(), channel);		
	}
	
	private Message preSentMessage(String type){
		Message msg = MessageFactory.getSingleton().createMessage(type);		
		//clock.sendAction();
		int stimeStamp = LamportLogicalClockService.getInstance().getValue();
		msg.setTimpeStamp(stimeStamp);
		msg.setNodeId(localId);
		return msg;
	}
	public void handlerRelease(MessageRelease message, int channel){
		if(!pqueue.isEmpty()){
			pqueue.poll();
		}		
	}
	
	public void handlerReply(MessageReply message, int channel){
		
	}
	
	@Override
	public synchronized void  csEnter() {		
		String type = MessageFactory.getSingleton().typeRequest;
		MessageRequest msg = (MessageRequest) MessageFactory.getSingleton().createMessage(type);
		//be careful 
		//clock.sendAction();
		localRequestStamp.timeStamp = LamportLogicalClockService.getInstance().getValue();
		msg.setTimpeStamp(localRequestStamp.timeStamp );
		msg.setNodeId(localId);		
		pqueue.add(localRequestStamp);		
		MessageSenderService.getInstance().sendBroadCast(msg.toString());
		if(!(pqueue.peek().nodeId == localId && conditionL1.size()== numOfNode-1)){			
				try {
					this.wait();
				} catch (InterruptedException e) {				
					e.printStackTrace();
				}				
		}
		
		/*while(true){
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {				
				e.printStackTrace();
			}
			if(pqueue.peek().nodeId == localId && conditionL1.size()== numOfNode-1){
				break;
			}
		}*/
	}

	@Override
	public void csLeave() {
		// be careful of the order of localRequestStamp and conditionL1.
		localRequestStamp.timeStamp = Integer.MAX_VALUE;
		conditionL1.clear();		
		if(!pqueue.isEmpty()){
			pqueue.poll();
		}
		String type = MessageFactory.getSingleton().typeRelease;
		Message release = preSentMessage(type);		 
		MessageSenderService.getInstance().sendBroadCast(release.toString());			
	}
	
	public static void main(String [] arg){
		// this main is just used for test. it is not the main of the project.
		Comparator<TimeStampWithID> comparator = new TimeStampWithID(0,0);
		PriorityQueue<TimeStampWithID> pqueue = new PriorityQueue<TimeStampWithID>(20, comparator);
		
		for(int i = 1; i < 10; i ++){
			TimeStampWithID localRequestStamp = new TimeStampWithID(i,i); 
			pqueue.offer(localRequestStamp);
			localRequestStamp = new TimeStampWithID(i+2,i); 
			pqueue.offer(localRequestStamp);
		}
		
		while(!pqueue.isEmpty()){
			TimeStampWithID xx = pqueue.poll();
			System.out.println(xx.timeStamp + "," + xx.nodeId);
		}		
	}
}
