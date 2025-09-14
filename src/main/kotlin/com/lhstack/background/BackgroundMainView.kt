package com.lhstack.background

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.wm.impl.IdeBackgroundUtil
import com.intellij.ui.JBIntSpinner
import com.intellij.util.ui.JBUI
import org.apache.commons.lang.StringUtils
import org.jdesktop.swingx.VerticalLayout
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.*

class BackgroundMainView(val project: Project) : JPanel(), Disposable, SyncDataTopic {

    companion object {
        const val PREFIX = "JToolsBackground"
    }

    val pathTextField: JTextField
    val opacityIntSpinner: JBIntSpinner
    val opacitySlider: JSlider
    val flipComboBox: ComboBox<Flip>
    val fillComboBox: ComboBox<Fill>
    val anchorComboBox: ComboBox<Anchor>
    val state: AtomicBoolean = AtomicBoolean(true)
    val propertiesComponent = PropertiesComponent.getInstance()
    val enableButton = JButton("启用")
    val disableButton = JButton("停用")

    init {
        val connect = ApplicationManager.getApplication().messageBus.connect(this)
        connect.subscribe(SyncDataTopic.TOPIC, this)
        this.layout = BoxLayout(this, BoxLayout.Y_AXIS)
        val backgroundImage = propertiesComponent.getValue("${PREFIX}.backgroundImage", "")
        pathTextField = JTextField(backgroundImage).apply {
            this.preferredSize = Dimension(Int.MAX_VALUE, 30)
            if (backgroundImage.isBlank()) {
                this.text = "点击右侧按钮选择你要设置的背景图片"
            }
        }
        val myPathField = TextFieldWithBrowseButton(pathTextField, object : ActionListener {
            override fun actionPerformed(e: ActionEvent) {
                val chooserDescriptor = FileChooserDescriptor(true, false, false, false, false, false)
                    .withFileFilter {
                        when (it.extension?.lowercase()) {
                            "png", "jpg", "jpeg", "gif", "svg" -> true
                            else -> false
                        }
                    }
                    .withTitle("背景图片")
                    .withDescription("选择你要设置的背景图片")
                val chooseFile = FileChooser.chooseFile(
                    chooserDescriptor,
                    project,
                    if(pathTextField.text.isNotBlank()){
                        VirtualFileManager.getInstance()
                            .findFileByUrl("file://${pathTextField.text}")
                    }else {
                        VirtualFileManager.getInstance()
                            .findFileByUrl("file://${System.getProperty("user.dir")}")
                    }
                )
                chooseFile?.let {
                    pathTextField.text = it.path
                    storeAndNotify("${PREFIX}.backgroundImage", it.path)
                }
            }

        })
        opacityIntSpinner = JBIntSpinner(propertiesComponent.getInt("${PREFIX}.opacity", 10), 0, 100)
        opacitySlider = JSlider(0, 100, propertiesComponent.getInt("${PREFIX}.opacity", 10))
        opacitySlider.addChangeListener {
            storeAndNotify("${PREFIX}.opacity", opacitySlider.value.toString())
            opacityIntSpinner.number = opacitySlider.value
        }
        opacityIntSpinner.addChangeListener {
            storeAndNotify("${PREFIX}.opacity", opacityIntSpinner.number.toString())
            opacitySlider.value = opacityIntSpinner.number
        }

        flipComboBox = ComboBox<Flip>(Flip.entries.toTypedArray()).apply {
            this.selectedItem = Flip.fromValue(propertiesComponent.getValue("${PREFIX}.flip", Flip.None.value))
            this.addItemListener {
                storeAndNotify("${PREFIX}.flip", (this.selectedItem as Flip).value)
            }
        }
        fillComboBox = ComboBox<Fill>(Fill.entries.toTypedArray()).apply {
            this.selectedItem = Fill.fromValue(propertiesComponent.getValue("${PREFIX}.fill", Fill.Plain.value))
            this.addItemListener {
                storeAndNotify("${PREFIX}.fill", (this.selectedItem as Fill).value)
            }
        }

        anchorComboBox = ComboBox<Anchor>(
            Anchor.entries.toTypedArray()
        ).apply {
            this.selectedItem = Anchor.fromValue(propertiesComponent.getValue("${PREFIX}.anchor", "center"))
            this.addItemListener {
                storeAndNotify("${PREFIX}.anchor", (this.selectedItem as Anchor).value)
            }
        }

        this.add(JScrollPane(JPanel(VerticalLayout()).apply {

            this.add(JLabel("  背景图片: ", JLabel.LEFT))
            this.add(myPathField)
            this.add(JPanel(BorderLayout()).apply {
                this.border = JBUI.Borders.empty(10, 0)
                this.add(JLabel(), BorderLayout.CENTER)
            })
            this.add(JLabel("  透明度: ", JLabel.LEFT))
            this.add(JPanel().apply {
                this.layout = BoxLayout(this, BoxLayout.X_AXIS)
                this.add(opacitySlider)
                this.add(opacityIntSpinner)
            })
            this.add(JPanel(BorderLayout()).apply {
                this.border = JBUI.Borders.empty(10, 0)
                this.add(JLabel(), BorderLayout.CENTER)
            })

            this.add(JLabel("  翻转设置: ", JLabel.LEFT))
            this.add(flipComboBox)

            this.add(JPanel(BorderLayout()).apply {
                this.border = JBUI.Borders.empty(10, 0)
                this.add(JLabel(), BorderLayout.CENTER)
            })
            this.add(JLabel("  填充方式: ", JLabel.LEFT))
            this.add(fillComboBox)
            this.add(JPanel(BorderLayout()).apply {
                this.border = JBUI.Borders.empty(10, 0)
                this.add(JLabel(), BorderLayout.CENTER)
            })

            this.add(JLabel("  锚点位置: ", JLabel.LEFT))
            this.add(anchorComboBox)
        }))

        this.add(Box.createVerticalBox())
        this.add(JPanel().apply {
            this.layout = BoxLayout(this, BoxLayout.X_AXIS)
            this.add(enableButton.apply {
                val enable = propertiesComponent.getBoolean("${PREFIX}.enable", false)
                this.isEnabled = !enable
                this.addActionListener {
                    if(pathTextField.text.isNotBlank()) {
                        val f = File(pathTextField.text)
                        if(!f.exists()){
                            Messages.showInfoMessage("背景图片不存在,请选择你要设置的背景图片","JToolsBackground")
                            return@addActionListener
                        }
                        if (!propertiesComponent.getBoolean("${PREFIX}.enable", false)) {
                            this.isEnabled = false
                            storeAndNotify("${PREFIX}.enable", "true")
                            disableButton.isEnabled = true
                            updateBackground()
                        }
                    }


                }
            })
            this.add(Box.createHorizontalGlue())
            this.add(disableButton.apply {
                val enable = propertiesComponent.getBoolean("${PREFIX}.enable", false)
                this.isEnabled = enable
                this.addActionListener {
                    if (propertiesComponent.getBoolean("${PREFIX}.enable", false)) {
                        this.isEnabled = false
                        storeAndNotify("${PREFIX}.enable", "false")
                        enableButton.isEnabled = true
                        clearBackground()
                    }
                }
            })
            this.border = JBUI.Borders.emptyBottom(20)
        })
    }

    fun updateBackground() {
        if (propertiesComponent.getBoolean("${PREFIX}.enable", false)) {
            pathTextField.text?.also {
                val file = File(it)
                if (!file.exists()) {
                    Messages.showErrorDialog("图片地址不存在,请检查图片是否被删除,或者切换图片地址", "JToolsBackground")
                    return@also
                }
                var path =
                    "${it},${opacitySlider.value},${(fillComboBox.selectedItem as Fill).value},${(anchorComboBox.selectedItem as Anchor).value}"
                if ((flipComboBox.selectedItem as Flip) != Flip.None) {
                    path = "${path},${(flipComboBox.selectedItem as Flip).value}"
                }
                propertiesComponent.setValue(IdeBackgroundUtil.FRAME_PROP, path)
                propertiesComponent.setValue(IdeBackgroundUtil.EDITOR_PROP, path)
                IdeBackgroundUtil.repaintAllWindows()
            }
        }
    }

    fun clearBackground() {
        propertiesComponent.unsetValue(IdeBackgroundUtil.FRAME_PROP)
        propertiesComponent.unsetValue(IdeBackgroundUtil.EDITOR_PROP)
    }

    override fun dispose() {

    }

    override fun update(project: Project) {
        //更新
        if (this.project.locationHash != project.locationHash) {
            state.set(false)

            pathTextField.text = propertiesComponent.getValue("${PREFIX}.backgroundImage", "")
            opacityIntSpinner.number = propertiesComponent.getInt("${PREFIX}.opacity", 10)
            opacitySlider.value = opacityIntSpinner.number
            flipComboBox.selectedItem = Flip.fromValue(propertiesComponent.getValue("${PREFIX}.flip", Flip.None.value))
            fillComboBox.selectedItem = Fill.fromValue(propertiesComponent.getValue("${PREFIX}.fill", Fill.Plain.value))
            anchorComboBox.selectedItem = Anchor.fromValue(propertiesComponent.getValue("${PREFIX}.anchor", "center"))
            enableButton.isEnabled = propertiesComponent.getBoolean("${PREFIX}.enable", false)
            disableButton.isEnabled = !enableButton.isEnabled
            //true: 更新properties
            state.set(true)
        }
    }

    fun storeAndNotify(key: String, value: String) {
        if (state.get()) {
            propertiesComponent.setValue(key, value)
            updateBackground()
            ApplicationManager.getApplication().messageBus.syncPublisher(SyncDataTopic.TOPIC).update(project)
        }
    }
}

enum class Anchor(val value: String, val remark: String) {
    TopLeft("top_left", "左上"),
    TopCenter("top_center", "中上"),
    TopRight("top_right", "右上"),
    MiddleLeft("middle_left", "中左"),
    Center("center", "居中"),
    MiddleRight("middle_right", "中右"),
    BottomLeft("bottom_left", "左下"),
    BottomCenter("bottom_center", "中下"),
    BottomRight("bottom_right", "右下");

    companion object {
        fun fromValue(value: String): Anchor {
            return Anchor.entries.firstOrNull { it.value == value } ?: Center
        }
    }

    override fun toString(): String = this.remark
}

enum class Fill(val value: String, val remark: String) {
    Plain("plain", "无"),
    Scale("scale", "拉伸"),
    Tile("tile", "平铺");

    companion object {
        fun fromValue(value: String): Fill {
            return Fill.entries.firstOrNull { it.value == value } ?: Plain
        }
    }

    override fun toString(): String = this.remark
}

enum class Flip(val value: String, val remark: String) {
    None("", "无"),
    FlipH("flipH", "水平翻转"),
    FlipV("flipV", "垂直翻转"),
    FlipHV("flipHV", "水平垂直翻转");

    companion object {
        fun fromValue(value: String): Flip {
            return Flip.entries.firstOrNull { it.value == value } ?: None
        }
    }

    override fun toString(): String = this.remark
}