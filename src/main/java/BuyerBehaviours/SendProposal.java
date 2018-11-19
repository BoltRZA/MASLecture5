package BuyerBehaviours;

import ETC.Book;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.DataStore;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.List;

import static ETC.Colours.*;


public class SendProposal extends Behaviour {
    private Agent agent;
    private AID bestSeller;
    private double bestPrice;
    private List<Book> bookList;
    private List<AID> receiversList;
    private boolean answerReceived = false;

    public SendProposal(Agent agent, DataStore ds, AID bestSeller, double bestPrice){
        this.bestPrice = bestPrice;
        this.bestSeller = bestSeller;
        this.agent = agent;
        this.receiversList = (List<AID>) ds.get("receiversList");
        this.bookList = (List<Book>) ds.get("bookList");
        setDataStore(ds);
    }
    @Override
    public void onStart(){
        super.onStart();
        ACLMessage proposal = new ACLMessage(ACLMessage.PROPOSE);
        proposal.addReceiver(bestSeller);
        proposal.setContent(bestPrice+"");
        proposal.setProtocol("tradeConfirm");
        agent.send(proposal);
        ACLMessage refuse = new ACLMessage(ACLMessage.REFUSE);
        refuse.setProtocol("tradeConfirm");
        for (AID rec: receiversList){
            if (rec!=bestSeller){
                refuse.addReceiver(rec);
            }
        }
        agent.send(refuse);
    }

    @Override
    public void action() {
        MessageTemplate mt = MessageTemplate.and(
                MessageTemplate.MatchSender(bestSeller),
                MessageTemplate.or(
                        MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL),
                        MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL)
                )
        );
        ACLMessage answer = agent.receive(mt);
        if (answer != null) {
            answerReceived = true;
            if (answer.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                System.out.println("Agent " + YELLOW + agent.getLocalName() + ZERO +
                        " said:" + GREEN + "I've got a confirm from " + BLUE + answer.getSender().getLocalName()
                        + ZERO + ", and bought a " + PURPLE + bookList.get(0).getTitle() + ZERO + " for " + bestPrice);
                bookList.remove(0);
                getDataStore().put("bookList", bookList);

            } else if (answer.getPerformative() == ACLMessage.REJECT_PROPOSAL) {
                System.out.println("Agent " + YELLOW + agent.getLocalName() + ZERO +
                        " said:"  + RED +"I've got a disconfirm from " + BLUE + answer.getSender().getLocalName()
                        + ZERO + ", i have to try again!" + ZERO);
                Book book = bookList.get(0);
                bookList.remove(0);
                bookList.add(book);
            }
        }
        else {
            block();
        }
    }

    @Override
    public boolean done() {

        return answerReceived;
    }

    @Override
    public int onEnd() {
        if (bookList.size() == 0){
            System.out.println("Agent " + YELLOW + agent.getLocalName() + ZERO +  " said:" +
                    GREEN + "I've finished bookbuying!");
        }
        else {
            System.out.println("Agent " + YELLOW + agent.getLocalName() + ZERO +
                    " said:" + CYAN + "There are still books in my list of purchase" + ZERO);
            System.out.println("----------------------------------------------------");
            agent.addBehaviour(new StartOfBuying(agent, getDataStore()));
        }
       return super.onEnd();
    }
}
