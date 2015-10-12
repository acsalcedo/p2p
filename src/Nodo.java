
import jade.core.Agent;
import jade.core.AID;
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
import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;



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
    private Documento script;
    private Documento codigo;
    /*private AID[] agente = {
            myAgent.getAID()
        };*/

    @Override
    protected void setup() {

        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        ServiceDescription sd2 = new ServiceDescription();

        Object[] args = getArguments();
        catalogo = new HashMap(10);

        // Solicitar un archivo
        if (args != null && args.length == 1) {
            archivoObjetivo = (String) args[0];
            System.out.println("Archivo objetivo: " + archivoObjetivo);
            addBehaviour(new BusquedaTransferenciaArchivos());

        // Solicitar cpu
        } else if (args != null && args.length > 1) {

            script = new Documento((String) args[0]);
            codigo = new Documento((String) args[1]);
            System.out.println("Script y codigo a distribuir: " + args[0] + " " + args[1]);

            addBehaviour(new BusquedaCPU());

        } else {
            // Valores iniciales para pruebas
            Documento archPrueba = new Documento("ejemplo1.txt");
            Documento archPrueba2 = new Documento("ejemplo2.txt");
            Documento archPrueba3 = new Documento("conversion.png");

            archPrueba2.incrDescargas();

            catalogo.put("ejemplo1.txt", archPrueba);
            catalogo.put("ejemplo2.txt", archPrueba2);
            catalogo.put("conversion.png", archPrueba3);
            System.out.println("Modo: Distribuidor");
        }


        dfd.setName(getAID());
        sd.setType("compartir_archivos");
        sd.setName("compartir_archivos");
        dfd.addServices(sd);

        sd2.setType("compartir_cpu");
        sd2.setName("compartir_cpu");
        dfd.addServices(sd2);

        // Para separar las clases posiblemente, se pasa por referencia los
        // atributos de Nodo a las clases Behaviour.
                // Registrar el servicio
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        System.out.println("Preparado para compartir archivos");
        System.out.println("Preparado para compartir CPU");

        addBehaviour(new ManejarSolicitudArchivos());
        addBehaviour(new ManejarTransferencia());
        addBehaviour(new ManejarSolicitudCPU());
        addBehaviour(new ManejarScriptCodigo());

    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        // Printout a dismissal message
        System.out.println("Nodo "+getAID().getName()+ " finalizado");
    }

    private class ManejarSolicitudArchivos extends CyclicBehaviour {
        MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchConversationId("Pedir_catalogo"),
                            MessageTemplate.MatchPerformative(ACLMessage.CFP));
        //private int estado = 0;

        @Override
        public void action() {
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                // CFP Message received. Process it
                System.out.println("Peticion recibida de: " + msg.getSender().getName());
                ACLMessage reply = msg.createReply();

                if (catalogo != null && (catalogo.get(msg.getContent()) != null)) {
                    System.out.println("Propose Archivo: " + msg.getContent());
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent(msg.getContent());
                    //estado = 1;

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

                            strMensaje += str + ":" + (catalogo.get(str)).getDescargas() + " ";
                        }
                    }
                    /* Si existe un archivo con el substring dado,
                       no manda un mensaje de refusal. */
                    if (archivoDisponible) {
                        System.out.println("Propose archivo: " + strMensaje);
                        reply.setPerformative(ACLMessage.PROPOSE);
                        reply.setContent(strMensaje);
                        //estado = 1;

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
        }

    }

    private class ManejarSolicitudCPU extends CyclicBehaviour {

        MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchConversationId("Pedir_cpu"),
                            MessageTemplate.MatchPerformative(ACLMessage.CFP));


        @Override
        public void action() {

            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                System.out.println("Peticion de CPU recibida de: " + msg.getSender().getName());
                ACLMessage reply = msg.createReply();

                reply.setPerformative(ACLMessage.PROPOSE);
                reply.setContent(Double.toString(getCpuPercentage()));
                myAgent.send(reply);
            } else {
                block();
            }
        }
    }

    private static double getCpuPercentage() {
        OperatingSystemMXBean osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        return osBean.getSystemCpuLoad() * 100;
    }


    private class ManejarTransferencia extends CyclicBehaviour {
        @Override
        public void action() {
            // Ignorar, además, los mensajes hacia si mismo
            /*MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL),
                                           MessageTemplate.not(MessageTemplate.MatchReceiver()));*/
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchConversationId("Transferencia"),
                               MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL));

            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                //Transferencia del archivo
                Documento doc = catalogo.get(msg.getContent());

                ACLMessage msgTransf = msg.createReply();
                msgTransf.setPerformative(ACLMessage.INFORM);
                msgTransf.setByteSequenceContent(doc.getContenidoByte());
                msgTransf.addUserDefinedParameter("file-name", msg.getContent());
                myAgent.send(msgTransf);
                doc.incrDescargas();
                System.out.println("Se realizo la transferencia del archivo.");

                //estado = 2;
            } else {
                block();
            }
        }
    }
    private class ManejarScriptCodigo extends Behaviour {

        private int estado = 0;
        private String scriptEjecutar = null;
        private String codigoEjecutar = null;

        @Override
        public void action() {

            switch (estado) {
            case 0:
                MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchConversationId("Script-Codigo"),
                                    MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL));


                ACLMessage msg = myAgent.receive(mt);
                if (msg != null) {
                    //Transferencia del archivo

                    String tipo = msg.getUserDefinedParameter("file-type");
                    String nombreArch = msg.getUserDefinedParameter("file-name");
                    File f = new File(tipo + "-" + nombreArch);
                    byte[] contenido = msg.getByteSequenceContent();
                    Documento nuevoArch = new Documento(nombreArch, contenido);

                    try {
                        FileOutputStream salida = new FileOutputStream(f);
                        salida.write(contenido);
                        salida.close();
                    } catch (Exception e) {
                        Logger.getLogger(Nodo.class.getName()).log(Level.SEVERE, null, e);
                    }

                    if (tipo.equals("script")) {
                        scriptEjecutar = nombreArch;
                        ACLMessage msgTransf = msg.createReply();
                        msgTransf.setConversationId("Script-Codigo");
                        msgTransf.setPerformative(ACLMessage.INFORM);
                        msgTransf.setContent("Mandar codigo");
                        myAgent.send(msgTransf);
                        System.out.println("Se recibio el script.");

                    } else if (tipo.equals("codigo")) {
                        codigoEjecutar = nombreArch;
                        System.out.println("Se recibio el codigo.");
                        estado = 1;
                    }

                } else {
                    block();
                }
            break;
            case 1:
                //TODO ejecutar codigo
                System.out.println("Ejecucion del codigo.");
            break;
            }
        }

        @Override
        public boolean done() {
            if (estado == 2) {
                System.out.println("Codigo ejecutado.");
                myAgent.doDelete();
            }

            return estado == 2;
        }
    }

    private class BusquedaTransferenciaArchivos extends Behaviour {
        private MessageTemplate mt;
        // Variables temporales para acceder al mejor distribuidor
        private AID mejorDistribuidor = null;
        private String[] archivosMejorDistribuidor = null;
        private boolean ningunResultado = false;
        private int estado = 0;
        private int nroRespuestas = 0;
        private int nroAgentesEncontrados = 0;
        private int nroArchivosEncontrados = 0;
        private String catalogo_solicitado = "";

        @Override
        public void action() {
            switch (estado) {
            case 0:
                DFAgentDescription template = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                sd.setType("compartir_archivos");
                template.addServices(sd);
                try {
                    // Enviar mensajes de solicitudes de archivos
                    DFAgentDescription[] result = DFService.search(myAgent, template);

                    if (result != null && result.length > 1) {
                        for (int i = 0; i < result.length; ++i) {
                            // Descartar bucle. Dado que se envia el mensaje a si
                            // mismo por su subscripción al servicio
                            if (!result[i].getName().equals( (Object) myAgent.getAID() )) {
                                cfp.addReceiver(result[i].getName());
                            }
                        }
                        cfp.setContent(archivoObjetivo);
                        cfp.setConversationId("Pedir_catalogo");
                        cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value

                        mt = MessageTemplate.and(MessageTemplate.MatchConversationId("Pedir_catalogo"),
                                           MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));

                        myAgent.send(cfp);
                        nroAgentesEncontrados = result.length-1;
                        //System.out.println("nroAgentesEncontrados " + nroAgentesEncontrados);
                        estado = 1;
                    } else {
                        System.out.println("No se encontró el servicio");
                        myAgent.doDelete();
                    }
                } catch (FIPAException fe) {
                    fe.printStackTrace();
                }

            break;
            case 1:
                // Recibe todo los mensajes y los guarda para el estado posterior
                ACLMessage reply = myAgent.receive(mt);
                if (reply != null) {
                    // Reply received
                    nroRespuestas += 1;

                    if (reply.getPerformative() == ACLMessage.PROPOSE) {
                        //Verificacion si la respuesta contiene mas de un nombre de archivo
                        String[] archivos;
                        archivos = (reply.getContent()).split(" ");
                        nroArchivosEncontrados += archivos.length;

                        for (int i = 0; i < archivos.length; ++i) {
                            catalogo_solicitado +=
                                reply.getSender().getName() + " -----> " + archivos[i] + "\n";
                        }

                        // Elige la respuesta más rapida. Solamente para probar
                        if (nroRespuestas == 1) {
                            mejorDistribuidor = reply.getSender();
                            archivosMejorDistribuidor = archivos;
                        }
                    }

                    if (nroRespuestas >= nroAgentesEncontrados) {
                        System.out.println("Numero de resultados: " + Integer.toString(nroRespuestas));
                        System.out.println("Numero de archivos encontrados: " + nroArchivosEncontrados);
                        System.out.println("CATALOGO SOLICITADO: \n" + catalogo_solicitado);
                        estado = 2;
                    }
                }
            break;
            case 2:
                ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                if (mejorDistribuidor != null && archivosMejorDistribuidor != null
                        && archivosMejorDistribuidor.length > 0) {
                    System.out.println("Peer seleccionado: " + mejorDistribuidor.getName());
                    order.addReceiver(mejorDistribuidor);

                    //Elige el archivo mas descargado

                    String archivoMasDescargado = null;
                    int masDescargas = -1;

                    for (int i = 0; i < archivosMejorDistribuidor.length; ++i) {

                        String[] archDescagas = (archivosMejorDistribuidor[i]).split(":");

                        int descargas = Integer.parseInt(archDescagas[1]);

                        if (descargas > masDescargas) {
                            archivoMasDescargado = archDescagas[0];
                            masDescargas = descargas;
                        }
                    }

                    order.setContent(archivoMasDescargado);
                    order.setConversationId("Transferencia");
                    order.setReplyWith("order" + System.currentTimeMillis());
                    myAgent.send(order);
                    // Prepare the template to get the purchase order reply
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("Transferencia"),
                                           MessageTemplate.MatchInReplyTo(order.getReplyWith()));
                    estado = 3;
                } else {
                    ningunResultado = true;
                }
            break;
            case 3:
                // Recibir el archivo enviado
                reply = myAgent.receive(mt);
                if (reply != null) {
                    if (reply.getPerformative() == ACLMessage.INFORM) {

                        // Escribir al sistema de archivos y
                        // agregarlo al catalogo del agente
                        String nombreArch = reply.getUserDefinedParameter("file-name");
                        File f = new File("solicitante-" + nombreArch);
                        byte[] contenido = reply.getByteSequenceContent();
                        Documento nuevoArch = new Documento(nombreArch, contenido);

                        catalogo.put(nombreArch, nuevoArch);

                        try {
                            FileOutputStream salida = new FileOutputStream(f);
                            salida.write(contenido);
                            salida.close();
                        } catch (Exception e) {
                            Logger.getLogger(Nodo.class.getName()).log(Level.SEVERE, null, e);
                            //System.out.println("Exception; BusquedaTransferenciaArchivos: "
                                               //+ e.getMessage());
                        }


                        System.out.println("Nuevo Archivo: " + nombreArch);
                    }

                    estado = 4;
                } else {
                    block();
                }
            break;
            }
        }

        @Override
        public boolean done() {
            if (estado == 4) {
                System.out.println("Archivo copiado.");
                //myAgent.doDelete();
            }
            if (ningunResultado) {
                myAgent.doDelete();
            }

            return estado == 4 || ningunResultado;
        }
    }



    private class BusquedaCPU extends Behaviour {
        private MessageTemplate mt;
        private boolean ningunResultado = false;
        private int estado = 0;
        private int nroRespuestas = 0;
        private int nroAgentesEncontrados = 0;
        private String cpuDisponible = "";


        @Override
        public void action() {
            switch (estado) {
            case 0:
                DFAgentDescription template = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                sd.setType("compartir_cpu");
                template.addServices(sd);
                try {
                    // Enviar mensajes de solicitudes de cpu
                    DFAgentDescription[] result = DFService.search(myAgent, template);

                    if (result != null && result.length > 1) {
                        for (int i = 0; i < result.length; ++i) {
                            // Descartar bucle. Dado que se envia el mensaje a si
                            // mismo por su subscripción al servicio
                            if (!result[i].getName().equals( (Object) myAgent.getAID() )) {
                                cfp.addReceiver(result[i].getName());
                            }
                        }
                        //cfp.setContent(script);
                        cfp.setConversationId("Pedir_cpu");
                        cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value

                        mt = MessageTemplate.and(MessageTemplate.MatchConversationId("Pedir_cpu"),
                                           MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));

                        myAgent.send(cfp);
                        nroAgentesEncontrados = result.length-1;
                        estado = 1;
                    } else {
                        System.out.println("No se encontró el servicio");
                        myAgent.doDelete();
                    }
                } catch (FIPAException fe) {
                    fe.printStackTrace();
                }

            break;
            case 1:
                // Recibe todo los mensajes y los guarda para el estado posterior
                ACLMessage reply = myAgent.receive(mt);
                if (reply != null) {
                    // Reply received
                    nroRespuestas += 1;

                    if (reply.getPerformative() == ACLMessage.PROPOSE) {

                        double porcentajeCPU = Double.parseDouble(reply.getContent());

                        cpuDisponible +=
                            reply.getSender().getName() + " con % CPU utilizado: " + reply.getContent() + " %\n";

                        if (porcentajeCPU < 40) {
                            ACLMessage scriptMsg = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                            scriptMsg.addReceiver(reply.getSender());
                            scriptMsg.setConversationId("Script-Codigo");
                            scriptMsg.addUserDefinedParameter("file-type", "script");
                            scriptMsg.addUserDefinedParameter("file-name", script.getNombre());
                            scriptMsg.setByteSequenceContent(script.getContenidoByte());
                            myAgent.send(scriptMsg);

                            System.out.println("Se realizo la transferencia del script.");

                            mt = MessageTemplate.and(MessageTemplate.MatchConversationId("Script-Codigo"),
                                                MessageTemplate.MatchPerformative(ACLMessage.INFORM));
                            estado = 2;
                        }
                    }

                    if (nroRespuestas >= nroAgentesEncontrados) {
                        System.out.println("Numero de resultados: " + Integer.toString(nroRespuestas));
                        System.out.println("CPU Disponible: \n" + cpuDisponible);
                        //estado = 2;
                    }

                } else {
                    block();
                }
            break;
            case 2:
                // Recibir el confirmacion del envio del script
                reply = myAgent.receive(mt);

                if (reply != null) {
                    if (reply.getPerformative() == ACLMessage.INFORM) {

                        ACLMessage codigoMsg = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                        codigoMsg.addReceiver(reply.getSender());
                        codigoMsg.setConversationId("Script-Codigo");
                        codigoMsg.addUserDefinedParameter("file-type", "codigo");
                        codigoMsg.addUserDefinedParameter("file-name", codigo.getNombre());
                        codigoMsg.setByteSequenceContent(codigo.getContenidoByte());
                        myAgent.send(codigoMsg);

                        System.out.println("Se realizo la transferencia del codigo.");
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
                System.out.println("Script final.");
                myAgent.doDelete();
            }

            return estado == 3;
        }
    }
}
