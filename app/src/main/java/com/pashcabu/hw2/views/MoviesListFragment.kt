package com.pashcabu.hw2.views

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.doOnPreDraw
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.work.WorkManager
import com.google.android.material.transition.MaterialContainerTransform
import com.pashcabu.hw2.R
import com.pashcabu.hw2.model.ConnectionChecker
import com.pashcabu.hw2.model.data_classes.Database
import com.pashcabu.hw2.model.data_classes.networkResponses.Movie
import com.pashcabu.hw2.view_model.*
import com.pashcabu.hw2.views.adapters.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.Dispatcher


class MoviesListFragment : Fragment(), SwipeRefreshLayout.OnRefreshListener {


//    private var openMovieListener: MoviesListClickListener = object : MoviesListClickListener {
//        override fun onMovieSelected(
//            movieID: Int,
//            title: String,
//            view: View
//        ) {
//            val detailsFragment = MovieDetailsFragment.newInstance(movieID)
//            val bundle = detailsFragment.arguments ?: Bundle()
//            bundle.putString("transition_name", view.transitionName)
//            detailsFragment.arguments = bundle
//            detailsFragment.sharedElementEnterTransition = MaterialContainerTransform().apply {
//                duration = 500
//                scrimColor = Color.TRANSPARENT
//            }
//            detailsFragment.sharedElementReturnTransition = MaterialContainerTransform().apply {
//                duration = 500
//                scrimColor = Color.TRANSPARENT
//            }
//
//            parentFragment?.let {
//                activity?.supportFragmentManager?.beginTransaction()
//                    ?.setReorderingAllowed(true)
//                    ?.replace(
//                        R.id.fragment_container,
//                        detailsFragment
//                    )
//                    ?.hide(it)
//                    ?.addToBackStack(title)
//                    ?.addSharedElement(view, view.transitionName)
//                    ?.commit()
//            }
//        }
//
//        override fun onMovieLiked(movie: Movie) {
//            viewModel.onLikedButtonPressed(endpoint, movie)
//        }
//    }

    private var movieListener: MoviesListAdapterInterface = object : MoviesListAdapterInterface {
        override fun onMovieSelected(id: Int, title: String, view: View) {
            val detailsFragment = MovieDetailsFragment.newInstance(id)
            val bundle = detailsFragment.arguments ?: Bundle()
            bundle.putString("transition_name", view.transitionName)
            detailsFragment.arguments = bundle
            detailsFragment.sharedElementEnterTransition = MaterialContainerTransform().apply {
                duration = 500
                scrimColor = Color.TRANSPARENT
            }
            detailsFragment.sharedElementReturnTransition = MaterialContainerTransform().apply {
                duration = 500
                scrimColor = Color.TRANSPARENT
            }

            activity?.supportFragmentManager?.beginTransaction()
                ?.setReorderingAllowed(true)
                ?.replace(
                    R.id.fragment_container,
                    detailsFragment
                )
                ?.addToBackStack(title)
                ?.addSharedElement(view, view.transitionName)
                ?.commit()

        }

        override fun onMovieLiked(movie: Movie) {
            viewModel.onLikedButtonPressed(endpoint, movie)
        }

    }

    //    private var adapter = NewMoviesListAdapter(openMovieListener)
    private val differentAdapter = MoviesListAdapter(movieListener)
    private lateinit var moviesListRecyclerView: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var offlineWarning: TextView
    private lateinit var roomDB: Database
    private lateinit var viewModel: MoviesListViewModel

    private lateinit var connectionViewModel: ConnectionViewModel
    private var endpoint: String? = null
    private var currentPage: Int = 1
    private var totalPages: Int = 0
    private var toast: Toast? = null
    private var connectionChecker: ConnectionChecker? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        postponeEnterTransition()
        view?.doOnPreDraw { startPostponedEnterTransition() }
        return inflater.inflate(R.layout.movies_list_fragment, container, false)

    }

    private fun findViews(view: View) {
        moviesListRecyclerView = view.findViewById(R.id.movies_list_recycler_view)
        moviesListRecyclerView.setHasFixedSize(true)
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh)
        swipeRefreshLayout.setOnRefreshListener {
            refreshData()
        }
        offlineWarning = view.findViewById(R.id.offline_warning)

    }

    private fun setUpAdapter(view: View) {
        val orientation = view.context.resources.configuration.orientation
        val width = view.context.resources.displayMetrics.widthPixels
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            moviesListRecyclerView.layoutManager = GridLayoutManager(context, 2)
            moviesListRecyclerView.addItemDecoration(Decorator().itemSpacing((width * 0.1 / 6).toInt()))
            moviesListRecyclerView.setPadding((width * 0.1 / 6).toInt())
        } else {
            moviesListRecyclerView.layoutManager = GridLayoutManager(context, 4)
            moviesListRecyclerView.addItemDecoration(Decorator().itemSpacing((width * 0.1 / 10).toInt()))
            moviesListRecyclerView.setPadding((width * 0.1 / 10).toInt())
        }
        moviesListRecyclerView.adapter = differentAdapter
    }

    private fun loadData(endpoint: String?) {
        viewModel.loadLiveData(endpoint, currentPage)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MOVIESLIST", "onCreate")
        roomDB = Database.createDB(requireContext())
        val worker = WorkManager.getInstance(requireContext())
        val factory = MyViewModelFactory.MoviesListViewModelFactory(roomDB, worker)
        val factoryConnection = MyViewModelFactory.ConnectionViewModelFactory()
        viewModel = ViewModelProvider(this, factory).get(MoviesListViewModel::class.java)
//        Log.d("ML", "ViewModel was created: $viewModel")
        connectionViewModel =
            ViewModelProvider(this, factoryConnection).get(ConnectionViewModel::class.java)
        connectionChecker = ConnectionChecker(requireContext())
    }

    override fun onResume() {
        super.onResume()
        Log.d(
            "ML", "$endpoint is onResume, " +
                    "view model is $viewModel"
        )

        if (endpoint == FAVOURITE) {
            viewModel.refreshMovieList(endpoint, currentPage)
        } else {
            viewModel.updateIfInFavourite(endpoint)
        }
    }

    private fun refreshData() {
        currentPage = 1
        viewModel.refreshMovieList(endpoint, currentPage)
    }

    private fun getEndpoint() {
        endpoint = arguments?.getString(ENDPOINT)
    }

    private fun subscribeLiveData() {
        val stateObserver = Observer<Boolean> {
            swipeRefreshLayout.isRefreshing = it
        }
        val listObserver = Observer<List<Movie?>> {
            Log.d("ML","Subscribed to a list, its size is ${it.size}")
            differentAdapter.submitList(it)

        }
        val pagesObserver = Observer<Int> {
            totalPages = it
        }
        val errorsObserver = Observer<String> {
            if (it != NO_ERROR) {
                if (toast == null) {
                    toast = Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT)
                    toast?.show()
                } else {
                    toast?.cancel()
                    toast = Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT)
                    toast?.show()
                }
            }
        }
        val connectionObserver = Observer<Boolean> {
            viewModel.setConnectionState(it)
            if (it) {
                offlineWarning.visibility = View.GONE
            } else {
                offlineWarning.visibility = View.VISIBLE
            }
        }
        viewModel.loadingState.observe(this.viewLifecycleOwner, stateObserver)
        viewModel.moviesList.observe(this.viewLifecycleOwner, listObserver)
        viewModel.amountOfPages.observe(this.viewLifecycleOwner, pagesObserver)
        viewModel.errorState.observe(this.viewLifecycleOwner, errorsObserver)
        viewModel.pageStep.observe(this.viewLifecycleOwner, {
            currentPage = it
        })
        connectionChecker?.observe(this.viewLifecycleOwner, {
            connectionViewModel.setConnectionState(it)
        })
        connectionViewModel.connectionState.observe(this.viewLifecycleOwner, connectionObserver)
    }

    private fun addLoadMoreListener() {
        val listener = BottomOfTheListListener {
            val pageToLoad = currentPage + 1
            if (totalPages != 0 && offlineWarning.visibility == View.GONE) {
                if (pageToLoad <= totalPages) {
                    viewModel.loadMore(endpoint, pageToLoad)
                } else {
                    Toast.makeText(this.context, "No more pages to load!", Toast.LENGTH_SHORT)
                        .show()
                }
            } else if (offlineWarning.visibility == View.VISIBLE) {
                Toast.makeText(this.context, "No connection!", Toast.LENGTH_SHORT)
                    .show()
            }
        }
        if (endpoint != FAVOURITE) {
            moviesListRecyclerView.addOnScrollListener(listener)
        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getEndpoint()
        findViews(view)
        loadData(endpoint)
        setUpAdapter(view)
        subscribeLiveData()
        addLoadMoreListener()
        addAnimationScrollListener()
    }

    private fun addAnimationScrollListener() {
//        moviesListRecyclerView.addOnScrollListener(adapter.AnimationScrollListener())
        moviesListRecyclerView.addOnScrollListener(differentAdapter.AnimationScrollListener())
    }

    override fun onRefresh() {
        swipeRefreshLayout.isRefreshing = true
        refreshData()
        swipeRefreshLayout.isRefreshing = false
    }

    override fun onPause() {
        super.onPause()
        toast?.cancel()
    }

    companion object {
        fun newInstance(endpoint: String): MoviesListFragment {
            val arguments = Bundle()
            arguments.putString(ENDPOINT, endpoint)
            val fragment = MoviesListFragment()
            fragment.arguments = arguments
            return fragment
        }

//        const val NOW_PLAYING = "now_playing"
//        const val POPULAR = "popular"
//        const val TOP_RATED = "top_rated"
//        const val UPCOMING = "upcoming"
        const val ENDPOINT = "EndPoint"
        const val FAVOURITE = "favourite"

    }
}




