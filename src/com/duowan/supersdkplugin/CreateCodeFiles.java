package com.duowan.supersdkplugin;

import com.intellij.ide.IdeView;
import com.intellij.ide.util.DirectoryChooserUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtilBase;
import org.w3c.dom.*;

import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by peter
 */
public class CreateCodeFiles extends AnAction {
    private Project project;
    private String moduleName="app";//默认是app模块
    private JDialog jFrame;
    JTextField mTextFieldSdkName;
    JTextField mTextFieldAuthorName;
    /*包名*/
    private String packagebase="";

    private String mSdkClassName;
    private String mAuthorName;




    public enum  CodeType {
        InitEntity("/Template/entity/TemplateInitEntity.txt","DwInitExtraEntity","entity"),
        LoginEntity("/Template/entity/TemplateLoginEntity.txt","ChannelLoginEntity","entity"),
        DepositEntity("/Template/entity/TemplateDepositEntity.txt","NormalDepositEntity","entity"),
        SdkAdapter("/Template/TemplateSdkAdapter.txt","",""),
        ApplicationAda("/Template/channel/TemplateApplicationAdapter.txt","ThirdSdkApplicationAda","/com/duowan/supersdk/channel"),
        Config("/Template/channel/TemplateConfig.txt","ChannelConfig","/com/duowan/supersdk/channel");
        private String path;
        private String simpleClassName;
        private String packageName; //以 /开始标识绝对包名，否则以相对包名
        private CodeType(String path,String mSimpleClassName,String packageName){
            this.path =path;
            this.simpleClassName = mSimpleClassName;
            this.packageName = packageName;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getSimpleClassName() {
            return simpleClassName;
        }

        public void setSimpleClassName(String simpleClassName) {
            this.simpleClassName = simpleClassName;
        }

        public String getPackageName() {
            return packageName;
        }

        public void setPackageName(String packageName) {
            this.packageName = packageName;
        }
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        project = event.getData(PlatformDataKeys.PROJECT);
        String projectpath =  project.getBasePath();
        System.out.println("projectPath = "+projectpath);
        IdeView view = (IdeView)event.getData(LangDataKeys.IDE_VIEW);
        if(view != null && project != null) {
            PsiDirectory directory = DirectoryChooserUtil.getOrChooseDirectory(view);
            if (directory != null) {
                moduleName= directory.getName();
            }
        }
        packagebase = readPackageName();
        initSelectView();

    }

    private void initSelectView() {
        jFrame = new JDialog();// 定义一个窗体Container container = getContentPane();
        jFrame.setModal(true);
        Container container = jFrame.getContentPane();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));

        JPanel panelname = new JPanel();// /定义一个面板
        panelname.setLayout(new GridLayout(2, 1));
        panelname.setBorder(BorderFactory.createTitledBorder("参数名称定义"));

        mTextFieldSdkName = new JTextField("请输入SDK桥接类名，不能为空 例如 YYBaiduSdkAdapter");
        panelname.add(mTextFieldSdkName);

        mTextFieldAuthorName = new JTextField("请输入注释的作者名称");
        panelname.add(mTextFieldAuthorName);
        container.add(panelname);

        JPanel menu = new JPanel();
        menu.setLayout(new FlowLayout());

        Button cancle = new Button();
        cancle.setLabel("取消");
        cancle.addActionListener(actionListener);

        Button ok = new Button();
        ok.setLabel("确定");
        ok.addActionListener(actionListener);
        menu.add(cancle);
        menu.add(ok);
        container.add(menu);


        jFrame.setSize(400, 200);
        jFrame.setLocationRelativeTo(null);
        jFrame.setVisible(true);
    }

    private String readPackageName() {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(project.getBasePath() +"/"+moduleName +"/src/main/AndroidManifest.xml");
            NodeList dogList = doc.getElementsByTagName("manifest");
            for (int i = 0; i < dogList.getLength(); i++) {
                Node dog = dogList.item(i);
                Element elem = (Element) dog;
                return elem.getAttribute("package");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private void saveAdapter(){
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            String androidMenifestFile =project.getBasePath() +"/"+moduleName +"/src/main/AndroidManifest.xml";
            Document doc = db.parse(androidMenifestFile);
            NodeList dogList = doc.getElementsByTagName("manifest");
            for (int i = 0; i < dogList.getLength(); i++) {
                Node dog = dogList.item(i);
                Element elem = (Element) dog;
                NodeList appNodeList  = elem.getElementsByTagName("application");
                for ( int j = 0;j <appNodeList.getLength();j++){
                    Node appNode = appNodeList.item(j);
                    Element childNode=doc.createElement("meta-data");
                    Attr attr1=doc.createAttribute("android:name");
                    attr1.setValue("THIRD_SDK_ADAPTER");
                    Attr attr2=doc.createAttribute("android:value");
                    attr2.setValue(packagebase+"."+mSdkClassName);
                    childNode.setAttributeNode(attr1);
                    childNode.setAttributeNode(attr2);
                    appNode.appendChild(childNode);
                }
            }
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(androidMenifestFile);
            transformer.transform(source, result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private ActionListener actionListener = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().equals("取消")) {
                jFrame.dispose();
            } else {

                mSdkClassName = mTextFieldSdkName.getText();
                mAuthorName = mTextFieldAuthorName.getText();

                if(mSdkClassName.isEmpty() || mAuthorName.isEmpty()){
                    Messages.showInfoMessage(project,"请补全参数","提示");
                    return;
                }
                jFrame.dispose();
                clickCreateFile();
                Messages.showInfoMessage(project,"正在生成代码，请稍后","提示");

            }

        }
    };


    private void clickCreateFile(){
        createFiles(CodeType.InitEntity);
        createFiles(CodeType.LoginEntity);
        createFiles(CodeType.DepositEntity);
        createFiles(CodeType.SdkAdapter);
        createFiles(CodeType.ApplicationAda);
        createFiles(CodeType.Config);
        saveAdapter();
    }


    /**
     * 创建文件
     */
    private void createFiles(CodeType codeType) {

        String content = "";

        String packagepath = packagebase.replace(".","/");
        String apppath = project.getBasePath() +"/"+moduleName +"/src/main/java/";
        String filename=codeType.getPath();
        String javaClassName = codeType.getSimpleClassName();
        String javaClassPackage= codeType.getPackageName();
        if (javaClassName.isEmpty()){
            javaClassName = mSdkClassName;
        }
        if (javaClassPackage.isEmpty()){
            javaClassPackage = packagebase;
        }else {
            if (javaClassPackage.startsWith("/")){
                javaClassPackage = javaClassPackage.replace("/",".");
            }else{
                javaClassPackage = packagebase+"."+ javaClassPackage.replace("/",".");
            }
        }




        content = ReadFile(filename);
        // 1.通用流程,处理顶部注释
        content  = dealFileTitle(content);
        content  = dealFilePackageName(content,javaClassPackage);
        content  = dealFileClassName(content,javaClassName);


        writetoFile(content,apppath+javaClassPackage.replace(".","/"), javaClassName+".java");




    }

    private String ReadFile(String filename){
        InputStream in = null;
        in = this.getClass().getResourceAsStream(filename);
        String content = "";
        try {
            content = new String(readStream(in));
        } catch (Exception e) {
        }
        return content;
    }


    /**
     * 处理注释
     *
     * @param content
     */
    private String dealFileTitle(String content) {
        content = content.replace("$author", mAuthorName);
        content = content.replace("$date", getNowDateShort());
        return content;
    }


    /**
     * 处理包名
     *
     * @param content
     */
    private String dealFilePackageName(String content,String packageName) {
        content = content.replace("$packagename", packageName);
        return content;
    }

    /**
     * 处理类名
     *
     * @param content
     */
    private String dealFileClassName(String content,String classname) {
        content = content.replace("$classname", classname);
        return content;
    }


    private String getNowDateShort() {
        Date currentTime = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        String dateString = formatter.format(currentTime);
        return dateString;
    }

    private void writetoFile(String content, String filepath, String filename) {
        try {
            File floder = new File(filepath);
            // if file doesnt exists, then create it
            if (!floder.exists()) {
                floder.mkdirs();
            }
            File file = new File(filepath + "/" + filename);
            if (!file.exists()) {
                file.createNewFile();
            }

            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(content);
            bw.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public byte[] readStream(InputStream inStream) throws Exception {
        ByteArrayOutputStream outSteam = new ByteArrayOutputStream();
        try {
            byte[] buffer = new byte[1024];
            int len = -1;
            while ((len = inStream.read(buffer)) != -1) {
                outSteam.write(buffer, 0, len);
                System.out.println(new String(buffer));
            }

        } catch (IOException e) {
        } finally {
            outSteam.close();
            inStream.close();
        }
        return outSteam.toByteArray();
    }

}