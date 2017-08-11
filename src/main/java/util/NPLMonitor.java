package util;
import jason.asSyntax.Literal;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Toolkit;
import java.io.IOException;
import java.io.StringWriter;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import npl.NPLInterpreter;


/** displays the NPL state in a GUI interface */
public class NPLMonitor {

    NPLInterpreter nengine;

    private static JFrame  frame;
    private static JTabbedPane allArtsPane;
    private static ScheduledThreadPoolExecutor updater = new ScheduledThreadPoolExecutor(1);
    private static int guiCount = 0;

    private JTabbedPane tpane;
    private JTextPane txtNF  = new JTextPane();
    private JTextPane txtNS  = new JTextPane();

    public NPLMonitor() {
    }

    private void initFrame() {
        frame = new JFrame("..:: NPL Inspector ::..");
        allArtsPane = new JTabbedPane(JTabbedPane.LEFT);

        frame.getContentPane().setLayout(new BorderLayout());
        frame.add(BorderLayout.CENTER, allArtsPane);
        frame.pack();

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        guiCount = guiCount+30;
        frame.setBounds(0, 0, 800, (int)(screenSize.height * 0.8));
        frame.setLocation((screenSize.width / 2)-guiCount - frame.getWidth() / 2, (screenSize.height / 2)+guiCount - frame.getHeight() / 2);
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                frame.setVisible(true);
            }
        });
    }

    public void add(String id, NPLInterpreter nengine) throws Exception {
        if (frame == null)
            initFrame();

        this.nengine = nengine;

        // normative state
        JPanel nsp = new JPanel(new BorderLayout());
        txtNS.setContentType("text/html");
        txtNS.setEditable(false);
        txtNS.setAutoscrolls(false);
        nsp.add(BorderLayout.CENTER, new JScrollPane(txtNS));

        // normative facts
        JPanel nFacts = new JPanel(new BorderLayout());
        txtNF.setContentType("text/plain");
        txtNF.setFont(new Font("courier", Font.PLAIN, 12));
        txtNF.setEditable(false);
        txtNF.setAutoscrolls(false);
        nFacts.add(BorderLayout.CENTER, new JScrollPane(txtNF));

        // center tabled
        tpane = new JTabbedPane();
        tpane.add("normative state", nsp);
        tpane.add("normative facts", nFacts);

        allArtsPane.add(id, tpane);

        updater.scheduleAtFixedRate(new Runnable() {
            public void run() {
                updateNS();
            }
        }, 0, 1, TimeUnit.SECONDS);

    }

    private String lastNSStr = "";
    private String lastNFStr = "";
    public void updateNS() {
        try {
            StringWriter so = new StringWriter();
            getNSTransformer().transform(new DOMSource(DOMUtils.getAsXmlDocument(nengine)), new StreamResult(so));
            String curStr = so.toString();
            if (! curStr.equals(lastNSStr))
                txtNS.setText(curStr);
            lastNSStr = curStr;

            StringBuilder out = new StringBuilder();
            out.append(nengine.getStateString());
            out.append("\nDump of facts:\n");
            for (Literal l: nengine.getAg().getBB())
                out.append("     "+l+"\n");
            curStr = out.toString();
            if (! curStr.equals(lastNFStr))
                txtNF.setText(curStr);
            lastNFStr = curStr;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private DocumentBuilder parser;
    public DocumentBuilder getParser() throws ParserConfigurationException {
        if (parser == null)
            parser = DOMUtils.getParser();
        return parser;
    }

    public Transformer getNSTransformer() throws TransformerConfigurationException, TransformerFactoryConfigurationError, IOException {
        return DOMUtils.getTransformerFactory().newTransformer(DOMUtils.getXSL("nstate"));
    }

}
