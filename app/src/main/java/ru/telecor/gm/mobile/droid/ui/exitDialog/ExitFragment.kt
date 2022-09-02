package ru.telecor.gm.mobile.droid.ui.exitDialog

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.dialog_route_result.*
import kotlinx.android.synthetic.main.dialog_route_result_new.*
import kotlinx.android.synthetic.main.dialog_route_result_new.btnExit
import kotlinx.android.synthetic.main.dialog_route_result_new.btnTryAgain
import kotlinx.android.synthetic.main.dialog_route_result_new.chart_photo
import kotlinx.android.synthetic.main.dialog_route_result_new.chart_route
import kotlinx.android.synthetic.main.dialog_route_result_new.data_preparation
import kotlinx.android.synthetic.main.dialog_route_result_new.recovery
import kotlinx.android.synthetic.main.dialog_route_result_new.text_route_completion
import kotlinx.android.synthetic.main.dialog_route_result_new.tv_result
import kotlinx.android.synthetic.main.dialog_route_result_new.tv_title
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import moxy.presenter.InjectPresenter
import moxy.presenter.ProvidePresenter
import ru.telecor.gm.mobile.droid.R
import ru.telecor.gm.mobile.droid.di.Scopes
import ru.telecor.gm.mobile.droid.entities.DriverInfo
import ru.telecor.gm.mobile.droid.extensions.visible
import ru.telecor.gm.mobile.droid.presentation.exitDialog.ExitDialogPresenter
import ru.telecor.gm.mobile.droid.presentation.exitDialog.ExitDialogView
import ru.telecor.gm.mobile.droid.ui.base.BaseFragment
import ru.telecor.gm.mobile.droid.ui.login.fragment.setting.settingFunctionality.SettingBottomSheetFragment
import ru.telecor.gm.mobile.droid.utils.ConnectivityUtils
import toothpick.Toothpick

class ExitFragment() : BaseFragment(), ExitDialogView {

    companion object {
        fun newInstance() = ExitFragment()
    }

    override val layoutRes = R.layout.dialog_route_result_new

    @InjectPresenter
    lateinit var presenter: ExitDialogPresenter

    @ProvidePresenter
    fun providePresenter(): ExitDialogPresenter {
        return Toothpick.openScope(Scopes.SERVER_SCOPE)
            .getInstance(ExitDialogPresenter::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initClick()
    }

    private fun initClick() {
        btnExit.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                if (!ConnectivityUtils.syncAvailability(requireContext())) {
                    requireActivity().runOnUiThread {
                        Toast.makeText(
                            requireContext(),
                            R.string.dialog_route_result_slow_internet_to_connection,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    requireActivity().runOnUiThread {
                        presenter.onExitButtonClicked()
                    }
                }
            }
        }

        btnTryAgain.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                if (!ConnectivityUtils.syncAvailability(requireContext())) {
                    requireActivity().runOnUiThread {
                        Toast.makeText(
                            requireContext(),
                            R.string.dialog_route_result_slow_internet_to_connection,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    presenter.onTryAgainButtonClicked()
                }
            }

        }

        btnSettings.setOnClickListener {
            presenter.onSettingsClicked()
        }
    }

    @SuppressLint("SetTextI18n")
    override fun showNumUploadPhoto(
        value: Float,
        loadCount: Int,
        maxCount: Int,
        destroyCount: Int,
        loadStatus: ExitDialogView.LoadStatus
    ) {
        value
        loadCount
        maxCount
        destroyCount

        chart_photo.apply() {
            val p = if ((value < 0) or (value > 100)) 100F else value
            if (p != 0.0f) {
                chart_photo.progress = p.toInt()
            }
            text_photo.text = "$loadCount / $maxCount"

            if (destroyCount > 0 && loadStatus == ExitDialogView.LoadStatus.FINiSH) {
                recovery.visibility = View.VISIBLE
                text_route_completion.text =
                    resources.getString(R.string.text_route_completion) + " " + destroyCount.toString()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun showEstimatedTimeOfUnloading(value: Int, loadStatus: ExitDialogView.LoadStatus) {

        var text = ""
        when (loadStatus) {

            ExitDialogView.LoadStatus.START -> {
                if (value == -1) {
                    text = resources.getString(R.string.estimated_time_not_calculate)
                    image_completion.isVisible = false
                } else {
                    val hour = value / 3600
                    val min = value / 60 % 60
                    val sec = value / 1 % 60
                    text = String.format(
                        "%s: %02d:%02d:%02d",
                        resources.getString(R.string.dialog_estimated_time_of_unloading),
                        hour,
                        min,
                        sec
                    )
                }
                setColor(text_completion, R.color.grey_color)
            }
            ExitDialogView.LoadStatus.FINiSH -> {
                text = resources.getString(R.string.estimated_time_all_load)
                setColor(text_completion, R.color.grey_color)
                image_completion.isVisible = true
            }

            ExitDialogView.LoadStatus.ERROR -> {
                text = resources.getString(R.string.estimated_time_load_error)
                setColor(text_completion, R.color.black_to_red_color)
                image_completion.isVisible = false
            }
        }
        text_completion.text = text
    }

    override fun showNumUploadRoute(value: Float, loadCount: Int, maxCount: Int) {
        text_tasks.text = "$loadCount / $maxCount"
        chart_route.apply() {
            val p = if ((value < 0) or (value > 100)) 100F else value
            chart_route.progress = p.toInt()
        }
    }

    override fun showSuccessDialog(driver: DriverInfo) {
        setDrawable(chart_photo, R.drawable.circle_background_layout)
        setDrawable(chart_route, R.drawable.circle_background_layout)
        tv_title.text =
            driver.lastName + " " + driver.firstName + " " + driver.middleName + " " + resources.getString(
                R.string.dialog_route_result_thanks
            )

        btnExit.visible(true)
        btnTryAgain.visible(false)
    }

    override fun showFailDialog(driver: DriverInfo) {
        setDrawable(chart_photo, R.drawable.circle_red_mark_background_stile)
        setDrawable(chart_route, R.drawable.circle_red_mark_background_stile)
        tv_title.text =
            driver.lastName + " " + driver.firstName + " " + driver.middleName + " " + resources.getString(
                R.string.dialog_route_result_thanks
            )

        btnTryAgain.visible(true)
        btnExit.visible(false)
    }

    override fun setLoadingState(value: ExitDialogView.WorkStatus) {
        val clickable = value == ExitDialogView.WorkStatus.ERROR
        btnTryAgain.visible(clickable)
        btnExit.visible(clickable)
        //сменил на постоянный true
//        pbLoading.visible(clickable)
//        pbLoading.visible(true)

        when (value) {
            ExitDialogView.WorkStatus.PREPARE -> {
                tv_result.text = getString(R.string.dialog_route_result_data_preparation)
                data_preparation.visibility = View.VISIBLE
                setColor(tv_result, R.color.black_color)
            }
            ExitDialogView.WorkStatus.UPLOAD -> {
                tv_result.text = getString(R.string.dialog_route_result_title_loading)
                data_preparation.visibility = View.VISIBLE
                setColor(tv_result, R.color.black_color)
            }
            ExitDialogView.WorkStatus.FINISH -> {
                tv_result.text = getString(R.string.dialog_route_result_title_result)
                data_preparation.visibility = View.INVISIBLE
                setColor(tv_result, R.color.black_color)
            }
            ExitDialogView.WorkStatus.ERROR -> {
                tv_result.text = getString(R.string.dialog_route_result_route_error)
                data_preparation.visibility = View.INVISIBLE
                setColor(tv_result, R.color.black_to_red_color)
            }
        }
    }

    override fun showMessage(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    override fun showLoadingRouteDialog() {
//        iv_status.setImageResource(R.drawable.ic_error_fill)
//        tv_title.text = resources.getString(R.string.dialog_route_result_route_completion)
//        btnTryAgain.visible(true)
//        btnExit.visible(false)
    }

    override fun showDataPreparationDialog() {
//        iv_status.setImageResource(R.drawable.ic_error_fill)
//        tv_title.text = resources.getString(R.string.dialog_route_result_route_completion)
//        btnTryAgain.visible(true)
//        btnExit.visible(false)
    }

    override fun showSettingsMenu() {
        val bottomSheetDialogFragment = SettingBottomSheetFragment()
        bottomSheetDialogFragment.show(requireActivity().supportFragmentManager, bottomSheetDialogFragment.tag)
    }

    private fun setDrawable(view: ProgressBar, drawable: Int){
        view.progressDrawable = ContextCompat.getDrawable(requireContext(), drawable)
    }

    private fun setColor(view: TextView, color: Int){
        view.setTextColor(ContextCompat.getColor(requireContext(), color))
    }
}
