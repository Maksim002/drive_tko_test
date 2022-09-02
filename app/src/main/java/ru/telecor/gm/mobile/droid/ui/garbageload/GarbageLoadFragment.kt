package ru.telecor.gm.mobile.droid.ui.garbageload

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.viewpager.widget.ViewPager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nhaarman.supertooltips.ToolTip
import kotlinx.android.synthetic.main.fragment_garbage_load.*
import kotlinx.android.synthetic.main.fragment_garbage_load.addressMessage
import kotlinx.android.synthetic.main.fragment_garbage_load.btnNavigator
import kotlinx.android.synthetic.main.fragment_garbage_load.btnSettings
import kotlinx.android.synthetic.main.fragment_garbage_load.btnToDiscovery
import kotlinx.android.synthetic.main.fragment_garbage_load.linearLayout
import kotlinx.android.synthetic.main.fragment_garbage_load.pbLoading
import kotlinx.coroutines.launch
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter
import ru.telecor.gm.mobile.droid.R
import ru.telecor.gm.mobile.droid.di.Scopes
import ru.telecor.gm.mobile.droid.entities.ContainerFailureReason
import ru.telecor.gm.mobile.droid.entities.ContainerLoadLevel
import ru.telecor.gm.mobile.droid.entities.db.ProcessingPhoto
import ru.telecor.gm.mobile.droid.entities.db.TaskExtended
import ru.telecor.gm.mobile.droid.entities.photo.GarbagePhotoModel
import ru.telecor.gm.mobile.droid.entities.processing.ProcessingStatusType
import ru.telecor.gm.mobile.droid.entities.processing.StandResult
import ru.telecor.gm.mobile.droid.entities.task.StatusTaskExtended
import ru.telecor.gm.mobile.droid.extensions.visible
import ru.telecor.gm.mobile.droid.model.PhotoType
import ru.telecor.gm.mobile.droid.presentation.garbageload.GarbageLoadPresenter
import ru.telecor.gm.mobile.droid.presentation.garbageload.GarbageLoadView
import ru.telecor.gm.mobile.droid.presentation.garbageload.util.GarbageLoadScreenState
import ru.telecor.gm.mobile.droid.ui.base.BaseFragment
import ru.telecor.gm.mobile.droid.ui.garbageload.fragment.fragments.ProblemFragment
import ru.telecor.gm.mobile.droid.ui.garbageload.fragment.fragments.SuccessfullyFragment
import ru.telecor.gm.mobile.droid.ui.garbageload.fragment.message.MessageErrorFragment
import ru.telecor.gm.mobile.droid.ui.garbageload.fragment.message.MessageErrorMassActionFragment
import ru.telecor.gm.mobile.droid.ui.garbageload.fragment.message.RouteCompletionFragment
import ru.telecor.gm.mobile.droid.ui.garbageload.pager.PhotoPagerAdapter
import ru.telecor.gm.mobile.droid.ui.garbageload.pager.SelectionPagerAdapter
import ru.telecor.gm.mobile.droid.ui.garbageload.rv.AdapterPhotoContainer
import ru.telecor.gm.mobile.droid.ui.garbageload.rv.GarbageContainerAdapter
import ru.telecor.gm.mobile.droid.ui.login.fragment.setting.settingFunctionality.SettingBottomSheetFragment
import ru.telecor.gm.mobile.droid.ui.photo.PhotoActivity
import ru.telecor.gm.mobile.droid.ui.photomake.PhotoMakeGeneralActivity
import ru.telecor.gm.mobile.droid.ui.phototrouble.PhotoTroubleActivity
import ru.telecor.gm.mobile.droid.ui.utils.LocationUtils
import ru.telecor.gm.mobile.droid.ui.viewingPhoto.ViewingPhotoFragment
import ru.telecor.gm.mobile.droid.utils.conect
import ru.telecor.gm.mobile.droid.utils.getPositionNetwork
import toothpick.Toothpick
import tyrantgit.explosionfield.ExplosionField
import ru.telecor.gm.mobile.droid.utils.Animation.slideDown
import ru.telecor.gm.mobile.droid.utils.Animation.slideUp

class GarbageLoadFragment : BaseFragment(), GarbageLoadView {
    private lateinit var listStatus: List<StatusTaskExtended>
    private lateinit var listContainer: List<ContainerLoadLevel>
    private lateinit var localFailureReasonsCache: List<ContainerFailureReason>
    private lateinit var adapters: GarbageContainerAdapter
    private lateinit var taskItem: StatusTaskExtended
    private var itemMore: ArrayList<StatusTaskExtended> = arrayListOf()
    private var rule = ""
    private var addressTxt = ""
    private var restartRecycler = true
    private var fragOpen = true
    private var repeatAnimation = false

    private var numberBar = 0
    private lateinit var params: LinearLayout.LayoutParams

    companion object {
        fun newInstance() = GarbageLoadFragment()
    }

    override val layoutRes = R.layout.fragment_garbage_load

    @InjectPresenter
    lateinit var presenter: GarbageLoadPresenter

    @ProvidePresenter
    fun providePresenter(): GarbageLoadPresenter {
        return Toothpick.openScope(Scopes.SERVER_SCOPE)
            .getInstance(GarbageLoadPresenter::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
        initClick()

        //RecyclerView
        adapters = GarbageContainerAdapter(object :
            GarbageContainerAdapter.GarbageListenerState {
            override fun setOnClickListener(
                item: StatusTaskExtended,
                list: ArrayList<StatusTaskExtended>
            ) {
                if (itemMore.firstOrNull { it.id == item.id } == null) {
                    itemMore.add(item)
                    presenter.detailedPhoto(itemMore)
                }

                if (restartRecycler) {
                    setRecyclerData(item)
                    restartRecycler = false
                }

                if (generalCheck.isChecked) {
                    disablingCheckbox()
                    generalCheck.isChecked = false
                    itemMore.clear()
                }
            }

            override fun setOnClickListenerCheck(
                isCheck: Boolean,
                item: StatusTaskExtended,
                list: ArrayList<StatusTaskExtended>
            ) {
                if (itemMore.size != 0) {
                    if (itemMore.indexOfFirst { it.id == item.id } != -1) {
                        itemMore.remove(item)
                        presenter.detailedPhoto(itemMore)
                    }
                }
                generalCheck.isChecked = false
                disablingCheckbox(list)
            }
        })

        //Временная мера для получения метода.
        btnToDiscovery.setOnClickListener {
            fragOpen = true
            presenter.taskDoneButtonClicked()
        }

        getPositionNetwork(requireActivity())

        btnPhotoBefore.setOnClickListener {
            presenter.photoBeforeButtonClicked(LocationUtils.getBestLocation(requireContext()))
        }
        btnPhotoAfter.setOnClickListener {
            presenter.photoAfterButtonClicked(LocationUtils.getBestLocation(requireContext()))
        }

        btnToRoute.setOnClickListener {
            presenter.getRoutes()
        }
        btnSettings.setOnClickListener { presenter.onSettingsClicked() }
        btnNavigator.setOnClickListener { presenter.routeButtonClicked() }
        onBeckPress.setOnClickListener {
            presenter.viewState.onBeckPressed()
        }
        btnPhotoProblem.setOnClickListener { presenter.photoProblemButtonClicked() }

        //Click масового выбора
        generalCheck.setOnClickListener {
            if (listStatus.size > 1) {
                if (rule == "") {
                    rule = listStatus[0].rule!!
                    saveValues()
                } else {
                    val status = listStatus.firstOrNull { it.rule != rule }
                    if (status != null) {
                        val bottomSheetDialogFragment = MessageErrorMassActionFragment()
                        bottomSheetDialogFragment.show(
                            requireActivity().supportFragmentManager,
                            bottomSheetDialogFragment.tag
                        )
                    } else {
                        saveValues()
                    }
                }
            } else {
                saveValues()
            }
            presenter.detailedPhoto(arrayListOf())
        }

        btnQrScanner.setOnClickListener {
            presenter.onQrCodeScannerButtonClicked()
        }

        containerMenu.setOnClickListener {
            setOptionsMenu(it)
        }
    }

    //Получает выподающий список и стутс выполения задания
    override fun setState(state: GarbageLoadScreenState) {
        listContainer = state.levelsList!!
        listStatus = state.list!!
        localFailureReasonsCache = state.localFailureReasonsCache!!

        //После полуния данных отклюбчает все че боксы.
        generalCheck.isChecked = false
        defaultValue(generalCheck.isChecked)
    }

    //Функция обработыки еденичный выбор или масовый
    private fun setRecyclerData(item: StatusTaskExtended? = null) {
        if (item == null) {
            if (searchCopies(listStatus) == "") {
                getRule(listStatus[0], "all")
            }
        } else {
            getRule(item, "none")
            taskItem = item
        }
    }

    //Получение данных поле тесных отношений клёцки с тепсиком
    private fun getRule(item: StatusTaskExtended? = null, name: String) {
        viewPagerRoute(item, name)
    }

    private fun defaultValue(boolean: Boolean) {
        generalCheck.isChecked = boolean
        adapters.setCheck(boolean)
        adapters.update(listStatus)
        rvGarbageContainers.adapter = adapters
    }

    private fun viewPagerRoute(item: StatusTaskExtended? = null, nameStatus: String) {
        orinDevice(3)
        if (nameStatus != "all") {
            if (item?.containerStatus?.containerFailureReason == null) {
                pagerStatus()
            } else {
                orinDevice(2)
            }
        } else {
            pagerStatus()
        }

        val adapter = SelectionPagerAdapter(childFragmentManager)
        adapter.addFragment(ProblemFragment(object : ProblemFragment.ProblemListener {
            override fun setOnClickListenerProgress(value: Int) {
                if (nameStatus == "all") {
                    presenter.onTroubleReasonChosen(
                        listStatus,
                        localFailureReasonsCache[value]
                    )
                } else if (nameStatus == "none") {
                    presenter.onTroubleReasonChosen(
                        itemMore,
                        localFailureReasonsCache[value]
                    )
                }
                itemMore.clear()
                booleanRepeat()
                conDisplaysFalse()
                presenter.saveRoute()
            }

            override fun setClickCancellation(boolean: Boolean) {
                defaultValue(boolean)
                booleanRepeat()
                conDisplaysFalse()
                itemMore.clear()
            }

            override fun setClearPhoto(photo: ProcessingPhoto) {
                presenter.setCleatPhoto(photo)
            }
        }, localFailureReasonsCache, item), "ПРОБЛЕМА")

        adapter.addFragment(SuccessfullyFragment(object :
            SuccessfullyFragment.SuccessfullyListener {
            override fun setOnClickListenerProgress(value: Double) {
                if (nameStatus == "all") {
                    var rt = true
                    listStatus.forEach {
                        if (it.rule == "SUCCESS_FAIL_MANUAL_VOLUME" ||
                            it.rule == "SUCCESS_FAIL_MANUAL_COUNT_WEIGHT"
                        ) {
                            if (rt) {
                                presenter.taskDoneButtonClicked(
                                    value.toString().toDoubleOrNull() ?: 0.0, 0, it.rule.toString()
                                )
                                rt = false
                            }
                        } else {
                            val listPosition =
                                listContainer.indexOfFirst { it.value == value }
                            if (listPosition != -1) {
                                presenter.elementLoadLevelChosen(
                                    it,
                                    listContainer[listPosition],
                                    false
                                )
                            }
                        }
                    }
//                    presenter.saveTaskDataDraft()

                } else if (nameStatus == "none") {
                    itemMore.forEach {
                        if (taskItem.rule == "SUCCESS_FAIL_MANUAL_VOLUME" ||
                            taskItem.rule == "SUCCESS_FAIL_MANUAL_COUNT_WEIGHT"
                        ) {
                            presenter.taskDoneButtonClicked(
                                value.toString().toDoubleOrNull() ?: 0.0,
                                it.id,
                                taskItem.rule.toString()
                            )
                        } else {
                            val listPosition = listContainer.indexOfFirst { it.value == value }
                            if (listPosition != -1) {
                                presenter.elementLoadLevelChosen(
                                    it,
                                    listContainer[listPosition],
                                    false
                                )
                            }
                        }
                    }
//                    presenter.saveTaskDataDraft()
                }
                itemMore.clear()
                booleanRepeat()
                conDisplaysFalse()
                presenter.saveRoute()
            }

            override fun setClickCancellation(boolean: Boolean) {
                defaultValue(boolean)
                booleanRepeat()
                conDisplaysFalse()
                itemMore.clear()
            }
        }, item = item), "УСПЕШНО")

        profilePager.adapter = adapter

        numButtonOne.setOnClickListener {
            profilePager.currentItem = 0
            adapter.notifyDataSetChanged()
            orinDevice(2)
        }

        numButtonTwo.setOnClickListener {
            profilePager.currentItem = 1
            adapter.notifyDataSetChanged()
            orinDevice(3)
        }

        profile_bar_zero.visibility = View.VISIBLE
        profile_bar_one.visibility = View.GONE

        profilePager.setOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                numberBar = position
            }

            override fun onPageSelected(position: Int) {
                val fadeIn = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
                val fadeOut = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_out)
                if (position != 1) {
                    profile_bar_zero.startAnimation(fadeIn)
                    profile_bar_one.startAnimation(fadeOut)
                    profile_bar_zero.visibility = View.VISIBLE
                    profile_bar_one.visibility = View.GONE
                } else {
                    profile_bar_one.startAnimation(fadeIn)
                    profile_bar_zero.startAnimation(fadeOut)
                    profile_bar_one.visibility = View.VISIBLE
                    profile_bar_zero.visibility = View.GONE
                }
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })
        conDisplaysTrue()
    }

    //Функция самастоятельно проверяет оринтацию
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            pagerCon()
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            pagerCon()
        }
    }

    private fun pagerCon() {
        if (profilePager.currentItem == 1) {
            orinDevice(3)
        } else {
            orinDevice(2)
        }
    }

    //При повороте устонавливает размеры
    private fun orinDevice(int: Int) {
        val paramsPager = profilePager.layoutParams as LinearLayout.LayoutParams
        val params = linearLayout.layoutParams as LinearLayout.LayoutParams
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            params.height = presenter.settingsPrefs.isLayoutWhite / int
            paramsPager.height = presenter.settingsPrefs.isLayoutWhite / int
        } else {
            params.height = presenter.settingsPrefs.isLayoutHeight / int
            paramsPager.height = presenter.settingsPrefs.isLayoutHeight / int
        }
    }

    private fun pagerStatus() {
        Handler().postDelayed({
            profilePager.currentItem = 1
            orinDevice(3)
        }, 20)
    }

    private fun searchCopies(list: List<StatusTaskExtended>): String {
        for (i in 0 until list.size - 1) {
            if (list[i].rule != list[i + 1].rule) {
                return list[i].rule.toString()
            }
        }
        return ""
    }

    //Закрывает оно с маршрутом
    private fun disablingCheckbox(list: ArrayList<StatusTaskExtended>) {
        if (list.size == 0) {
            defaultValue(false)
            booleanRepeat()
            conDisplaysFalse()
        }
    }

    //Закрывает оно с маршрутом
    private fun disablingCheckbox() {
        defaultValue(false)
        booleanRepeat()
        conDisplaysFalse()
    }

    private fun initClick() {
        //Всплывающее сообщение с адресом
        tvGarbageLoadAddress.setOnClickListener {
            if (addressTxt != "") {
                val toolTip = ToolTip()
                    .withText(addressTxt)
                    .withColor(resources.getColor(R.color.gray2))

                val message = addressMessage.showToolTipForView(toolTip, addressMessage)
                Handler().postDelayed({
                    message.remove()
                }, 1300)
            } else if (addressTxt == "") {
                showMessage("Адрес пусть")
            }
        }

        detailsBefore.setOnClickListener {
            presenter.photoBeforeButtonClicked(LocationUtils.getBestLocation(requireContext()))
        }
        detailsAfter.setOnClickListener {
            presenter.photoAfterButtonClicked(LocationUtils.getBestLocation(requireContext()))
        }
        detailsGallery.setOnClickListener {
            presenter.onAddProblemButtonClicked(LocationUtils.getBestLocation(requireContext()))
        }
    }

    private fun saveValues() {
        if (generalCheck.isChecked) {
            adapters.setCheck(true)
            generalCheck.isChecked = true
            adapters.update(listStatus)
            rvGarbageContainers.adapter = adapters
        } else {
            adapters.setCheck(false)
            generalCheck.isChecked = false
            adapters.update(listStatus)
            rvGarbageContainers.adapter = adapters
            conDisplaysFalse()
        }

        if (generalCheck.isChecked) {
            setRecyclerData()
        }
    }

    private fun setOptionsMenu(it: View) {
        val popupMenu = PopupMenu(requireActivity(), it)


        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.problemExportMenu -> {
                    presenter.onAddProblemButtonClicked(LocationUtils.getBestLocation(requireContext()))
                    true
                }
                R.id.bulkSiteMenu -> {
                    presenter.onAddBlockageButtonClicked(
                        LocationUtils.getBestLocation(
                            requireContext()
                        )
                    )
                    true
                }
                else -> false
            }
        }
        popupMenu.inflate(R.menu.bottom_bar_menu)
        popupMenu.show()
    }

    //Открытие настроек
    override fun showSettingsMenu(boolean: Boolean) {
        presenter.isVisibilityNext(View.VISIBLE)
        if (boolean) {
            val bottomSheetDialogFragment = SettingBottomSheetFragment()
            bottomSheetDialogFragment.show(
                requireActivity().supportFragmentManager,
                bottomSheetDialogFragment.tag
            )
            presenter.viewState.showSettingsMenu(false)
        }
    }

    override fun onBeckPressed() {
        requireActivity().onBackPressed()
    }

    override fun setBeforePhotoButtonCompleted() {
        btnPhotoBefore.backgroundTintList =
            ContextCompat.getColorStateList(requireContext(), R.color.colorAccent)
    }

    override fun setAfterPhotoButtonCompleted() {
        btnPhotoAfter.backgroundTintList =
            ContextCompat.getColorStateList(requireContext(), R.color.colorAccent)
    }

    override fun setProblemPhotoButtonCompleted() {
        btnPhotoProblem.backgroundTintList =
            ContextCompat.getColorStateList(requireContext(), R.color.design_default_color_error)
    }

    override fun setAddButtonVisibility(value: Boolean) {
        btnAddGarbageTask.visible(value)
    }

    override fun setTaskInfo(name: String) {
        tvGarbageLoadAddress.text = name
        addressTxt = name
    }

    //Анимирует скрытие иконки
    override fun setActionCompleted(boolean: Boolean?) {
        if (boolean == false) {
            if (usedMessageTxt.isVisible) {
                val mExplosionField = ExplosionField.attach2Window(requireActivity())
                if (usedMessageTxt.width != 0 || usedMessageTxt.height != 0) {
                    mExplosionField.clear()
                    mExplosionField.explode(usedMessageTxt)
                }
                usedMessageTxt.isVisible = boolean
            }
        }
    }

    // При старте проверю кнопку
    // Если нет выполненых заданий то жопа иначе ты мусорщик (решай сам)
    override fun setActionCompletedBottom(boolean: Boolean?) {
        if (boolean == false) {
            btnToDiscovery.isEnabled = boolean
            btnToDiscovery.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.gray
                )
            )
        } else {
            btnToDiscovery.isEnabled = boolean == true
            btnToDiscovery.background =
                ContextCompat.getDrawable(requireContext(), R.drawable.white_green_button_style)
        }
    }

    //Последовательность отоброжения полей при роверке фото
    private fun setActionCompletedPhoto(con: Boolean, res: Boolean) {
        layoutGalleryCon.visible(con)
        recyclerViewGarbage.visible(res)
    }

    override fun showContainerTroubleDialog(
        taskStatus: StatusTaskExtended,
        list: List<ContainerFailureReason>
    ) {
        MaterialAlertDialogBuilder(context!!)
            .setTitle(resources.getString(R.string.garbage_load_dialog_trouble_reason_label))
            .setItems(list.map { item -> item.name }.toTypedArray()) { _, position ->
                presenter.onTroubleReasonChosen(arrayListOf(taskStatus), list[position])
            }
            .show()
    }

    override fun showContainerLevelDialog(
        taskStatus: StatusTaskExtended,
        levelsList: List<ContainerLoadLevel>
    ) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Уровень загрузки")
            .setItems(levelsList.map { item -> item.title }.toTypedArray()) { _, position ->
                presenter.elementLoadLevelChosen(taskStatus, levelsList[position])
            }
            .show()
    }

    override fun showPhoto(model: ArrayList<GarbagePhotoModel>) {
        val adapters = AdapterPhotoContainer(deleteOption = { photo ->
            presenter.onPhotoDeleteClicked(photo)
        }, photo = {
            presenter.saveRoute()
//            presenter.getPhoto(it)
            photoPager.isVisible = true
            viewPager(it)
        })
        adapters.update(model)
        recyclerViewGarbage.adapter = adapters

        if (model.firstOrNull { it.type != "TASK_TROUBLE" } != null) {
            setActionCompletedPhoto(false, res = true)
        } else {
            setActionCompletedPhoto(true, res = false)
        }
    }

    private fun viewPager(photo: ProcessingPhoto? = null) {
        val adapter = PhotoPagerAdapter(childFragmentManager)
        adapter.addFragment(
            ViewingPhotoFragment(
                photo,
                true,
                object : ViewingPhotoFragment.Listener {
                    override fun setOnClickClearPhoto() {
                        photoPager.isVisible = false
                    }
                })
        )
        photoPager.adapter = adapter
    }

    override fun setLoadingState(value: Boolean) {
        pbLoading.visible(value)
    }

    //Отоброжает различные сообщения
    override fun textMessageError(textMessage: String) {
        if (fragOpen) {
            val bottomSheetDialogFragment = MessageErrorFragment(textMessage)
            bottomSheetDialogFragment.show(
                requireActivity().supportFragmentManager,
                bottomSheetDialogFragment.tag
            )
            fragOpen = false
        }
    }

    //Отоброжение скрытие контенера
    override fun setHidingPanelValid(boolean: Boolean) {
        if (repeatAnimation != boolean){
            if (!boolean){
                repeatAnimation = boolean
                slideDown(conPhotoDet)
            }else{
                repeatAnimation = boolean
                slideUp(conPhotoDet)
            }
        }
//        conPhotoDet.isVisible = boolean
    }

    //Если данные по маршруту верны едим дальше
    override fun setCompleteRoute(
        localTaskCache: TaskExtended,
        statusType: ProcessingStatusType,
        standResults: MutableList<StandResult>
    ) {
        val bottomSheetDialogFragment =
            RouteCompletionFragment(object : RouteCompletionFragment.ListenerCompleteRoute {
                override fun setOnClickRoute() {
                    presenter.launch {
                        presenter.routeInteractor.addTaskToProcessing(
                            localTaskCache,
                            statusType,
                            standResults,
                            null
                        )
                        presenter.router.exit()
                    }
                }
            }, localTaskCache.stand?.address)
        bottomSheetDialogFragment.show(
            requireActivity().supportFragmentManager,
            bottomSheetDialogFragment.tag
        )
    }

    override fun startExternalCameraForResult(path: String) {
        presenter.getStatusPhoto(true)
        getCamera(path)
    }

    //Opening the camera
    private fun getCamera(path: String) {
        withPermission(android.Manifest.permission.CAMERA, {
            if (conect(requireContext(), requireActivity())) {
                startActivityForResult(
                    PhotoMakeGeneralActivity.createIntent(
                        requireActivity(),
                        path,
                        presenter.photoType,
                        presenter.conId
                    ),
                    1
                )
            }
        },
            { showMessage(getString(R.string.error_permission_denied)) })
    }

    override fun takePhoto(routeCode: String, taskId: String, type: PhotoType) {
        startActivityForResult(
            activity?.let { it1 ->
                PhotoActivity.createIntent(
                    it1,
                    type,
                    routeCode,
                    taskId
                )
            }, type.ordinal
        )
    }

    override fun takeProblemPhoto(routeCode: String, taskId: String) {
        startActivityForResult(
            activity?.let { it1 ->
                PhotoTroubleActivity.createIntent(
                    it1,
                    routeCode,
                    taskId
                )
            }, 27
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                PhotoType.LOAD_AFTER.ordinal -> {
                }
                PhotoType.LOAD_BEFORE.ordinal -> {
                }
                PhotoType.LOAD_TROUBLE.ordinal -> {
                }
            }
        }
    }

    private fun booleanRepeat() {
        presenter.isCheck = false
        restartRecycler = true
    }

    private fun conDisplaysTrue() {
        bottomLayoutCon.isVisible = true
    }

    private fun conDisplaysFalse() {
        bottomLayoutCon.isVisible = false
    }

    override fun setContainerAction(action: String) {
        tvContainerAction.text = action
    }
}
