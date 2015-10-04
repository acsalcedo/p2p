
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.io.File;
import java.util.HashMap;

/**
 * Modulo: Agente Nodo
 * Descripcion: Implementa el envio y recepcion de archivos.
 * Sistema P2P para compartición de archivos
 * @version 1.0
 * @author Daniel Leones 09-10977
 * @author Andrea Salcedo 10-10666
 */
public class Nodo extends Agent {
    private HashMap <String,File> catalogo;
    private String archivoObjetivo;
//    private Set<AID> agentes

    @Override
    protected void setup() {
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();

        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            archivoObjetivo = (String) args[0];
            System.out.println("Archivo objetivo: " + archivoObjetivo);
            System.out.println("Modo: Solicitante");
/*            dfd.setName(getAID());
            sd.setType("Solicitante");
            sd.setName("compartir_archivos");
            dfd.addServices(sd);*/
            addBehaviour(new PedirCatalago());

        } else {
            // Valores iniciales
            File archPrueba = new File("ProySOP3.pdf");
            catalogo = new HashMap(10);
            catalogo.put("ProySOP3.pdf", archPrueba);
            System.out.println("Modo: Distribuidor");

            dfd.setName(getAID());
            sd.setType("Publicista");
            sd.setName("compartir_archivos");
            dfd.addServices(sd);

            addBehaviour(new SolicitudArchivos());
            // Para separar las clases posiblemente, se pasa por referencia los
            // atributos de Nodo a las clases Behaviour.
                    // Registrar el servicio
            try {
                DFService.register(this, dfd);
            }
            catch (FIPAException fe) {
                fe.printStackTrace();
            }
            System.out.println("Preparado para compartir archivos");
        }
    }

    @Override
    protected void takeDown() {
        // Printout a dismissal message
        System.out.println("Nodo "+getAID().getName()+ " finalizado");
    }

    private class SolicitudArchivos extends CyclicBehaviour {
        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage msg = myAgent.receive(mt);
    	    if (msg != null) {
                // CFP Message received. Process it
                System.out.println("Peticion recibida de: " + msg.getSender().getName());
                ACLMessage reply = msg.createReply();
                if (catalogo != null && (catalogo.get(msg.getContent())!= null)) {
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent(msg.getContent());
                }
                else {
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("Ningun archivo disponible");
                }
                myAgent.send(reply);
    	    }
            else {
              block();
            }
        }
    }

    private class PedirCatalago extends Behaviour {
        private MessageTemplate mt;
        private int estado = 0;
        private int nroRespuestas = 0;
        private int nroAgentesEncontrados = 0;
        private String catalogo_solicitado = "";

        @Override
        public void action() {
            switch (estado) {
            case 0:
                DFAgentDescription template = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                sd.setType("Publicista");
                template.addServices(sd);
                try {
                    DFAgentDescription[] result = DFService.search(myAgent, template);
                    // Send the cfp to all sellers
                    if ( result != null && result.length > 0) {
                        for (int i = 0; i < result.length; ++i) {
                            cfp.addReceiver(result[i].getName());
                        }
                        cfp.setContent(archivoObjetivo);
                        cfp.setConversationId("Pedir_catalogo");
                        cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value

                        mt = MessageTemplate.and(MessageTemplate.MatchConversationId("Pedir_catalogo"),
                                           MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));

                        myAgent.send(cfp);
                        nroAgentesEncontrados = result.length;
                        estado = 1;
                    } else {
                        System.out.println("No se encontro el servicio");
                        myAgent.doDelete();
                    }

                } catch (FIPAException fe) {
                    fe.printStackTrace();
                }

                break;
            case 1:
                // Receive all proposals/refusals from seller agents
                ACLMessage reply = myAgent.receive(mt);
                if (reply != null) {
                    // Reply received
                    nroRespuestas += 1;
                    if (nroRespuestas >= nroAgentesEncontrados) {
                        estado = 2;
                    }
                    if (reply.getPerformative() == ACLMessage.PROPOSE) {
                        // This is an offer
                        catalogo_solicitado += "Numero de resultados: " + Integer.toString(nroRespuestas) + "\n";
                        catalogo_solicitado +=
                            reply.getSender().getName() + " -----> " + reply.getContent() + "\n";
                    }

                    if (catalogo_solicitado.isEmpty()) {
                        catalogo_solicitado = "No se encontraron resultados";
                    }
                }
                else {
                    block();
                }
                break;
            }
        }

        @Override
        public boolean done() {
            if ( estado == 2 ) {
                System.out.println("CATALOGO SOLICITADO: \n" + catalogo_solicitado);
                myAgent.doDelete();
            }
            return estado == 2  ;
        }

    }

}
