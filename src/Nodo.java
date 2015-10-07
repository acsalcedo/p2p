
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Modulo: Agente Nodo
 * Descripcion: Implementa el envio y recepcion de archivos.
 * Sistema P2P para compartición de archivos
 * @version 1.0
 * @author Daniel Leones 09-10977
 * @author Andrea Salcedo 10-10666
 */
public class Nodo extends Agent {
    private HashMap <String,Documento> catalogo;
    private String archivoObjetivo;

    @Override
    protected void setup() {

        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();

        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            archivoObjetivo = (String) args[0];
            System.out.println("Archivo objetivo: " + archivoObjetivo);
            System.out.println("Modo: Solicitante");
            addBehaviour(new PedirCatalago());

        } else {
            // Valores iniciales
            Documento archPrueba = new Documento("ejemplo1.txt");
            Documento archPrueba2 = new Documento("ejemplo2.txt");
            Documento archPrueba3 = new Documento("conversion.png");

            catalogo = new HashMap(10);

            catalogo.put("ejemplo1.txt", archPrueba);
            catalogo.put("ejemplo2.txt", archPrueba2);
            catalogo.put("conversion.png", archPrueba3);
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
            } catch (FIPAException fe) {
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

    private class SolicitudArchivos extends Behaviour {
        MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
        private int estado = 0;

        @Override
        public void action() {
            switch (estado) {
                case 0:
                    ACLMessage msg = myAgent.receive(mt);
                    if (msg != null) {
                        // CFP Message received. Process it
                        System.out.println("Peticion recibida de: " + msg.getSender().getName());
                        ACLMessage reply = msg.createReply();

                        if (catalogo != null && (catalogo.get(msg.getContent())!= null)) {
                            System.out.println("Propose Archivo: " + msg.getContent());
                            reply.setPerformative(ACLMessage.PROPOSE);
                            reply.setContent(msg.getContent());
                            estado = 1;

                        } else {

                            // Busca si existen archivos que contiene el substring dado
                            Iterator it = (catalogo.keySet()).iterator();
                            boolean archivoDisponible = false;
                            String strMensaje = "";

                            while (it.hasNext()) {

                                String str = (it.next()).toString();

                                /* Si existe un archivo con el substring dado,
                                   agrega el nombre del archivo al mensaje. */
                                if (str != null && str.contains(msg.getContent())) {

                                    archivoDisponible = true;
                                    strMensaje += str + " ";
                                }
                            }
                            /* Si existe un archivo con el substring dado,
                               no manda un mensaje de refusal. */
                            if (archivoDisponible) {
                                System.out.println("Propose archivo: " + strMensaje);
                                reply.setPerformative(ACLMessage.PROPOSE);
                                reply.setContent(strMensaje);
                                estado = 1;

                            } else {
                                System.out.println("Refuse");
                                reply.setPerformative(ACLMessage.REFUSE);
                                reply.setContent("Ningun archivo disponible");
                            }
                        }
                        myAgent.send(reply);
                    } else {
                        block();
                    }
                break;
                case 1:
                    mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
                    msg = myAgent.receive(mt);

                    if (msg != null) {
                        //Transferencia del archivo
                        Documento doc = catalogo.get(msg.getContent());

                        try {
                            ACLMessage msgTransf = msg.createReply();
                            msgTransf.setPerformative(ACLMessage.INFORM);
                            msgTransf.setByteSequenceContent(doc.getContenidoByte());
                            msgTransf.addUserDefinedParameter("file-name", msg.getContent());
                            myAgent.send(msgTransf);
                            doc.incrDescargas();
                            System.out.println("Se realizo la transferencia del archivo.");

                        } catch (IOException ex) {
                            Logger.getLogger(Nodo.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        estado = 2;
                    } else {
                        block();
                    }
                break;
            }
        }

        public boolean done(){
            if (estado == 2) {
                mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
                estado = 0;
            }
            return estado == 2;
        }
    }

    private class PedirCatalago extends Behaviour {
        private MessageTemplate mt;
        private int estado = 0;
        private int nroRespuestas = 0;
        private int nroAgentesEncontrados = 0;
        private int selector = 0; // variable temporal para seleccionar un archivo
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
                    if (result != null && result.length > 0) {
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

                        //Verificacion si la respuesta contiene mas de un nombre de archivo
                        String[] archivos;
                        archivos = (reply.getContent()).split(" ");

                        for (int i = 0; i < archivos.length; ++i) {
                            catalogo_solicitado +=
                                reply.getSender().getName() + " -----> " + archivos[i] + "\n";
                        }

                        System.out.println("Numero de archivos encontrados: " + archivos.length);
                        System.out.println("CATALOGO SOLICITADO: \n" + catalogo_solicitado);

                        ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                        order.addReceiver(reply.getSender());
                        order.setContent(archivos[0]);
                        order.setConversationId("Transferencia");
                        order.setReplyWith("order"+System.currentTimeMillis());
                        myAgent.send(order);
                        // Prepare the template to get the purchase order reply
                        mt = MessageTemplate.and(MessageTemplate.MatchConversationId("Transferencia"),
                                               MessageTemplate.MatchInReplyTo(order.getReplyWith()));

                    }
                }
            break;
            case 2:
                // Receive all proposals/refusals from seller agents
                reply = myAgent.receive(mt);
                if (reply != null) {

                    if (reply.getPerformative() == ACLMessage.INFORM) {

                        String nombreArch = reply.getUserDefinedParameter("file-name");
                        File f = new File("solicitante-" + nombreArch);
                        byte[] contenido = reply.getByteSequenceContent();

                        try {
                            FileOutputStream salida = new FileOutputStream(f);
                            salida.write(contenido);
                            salida.close();
                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                        }
                    }

                    if (catalogo_solicitado.isEmpty()) {
                        catalogo_solicitado = "No se encontraron resultados";
                    }
                    estado = 3;
                } else {
                    block();
                }
            break;
            }
        }

        @Override
        public boolean done() {
            if (estado == 3) {
                System.out.println("Archivo copiado.");
                myAgent.doDelete();
            }
            return estado == 3;
        }
    }
}
