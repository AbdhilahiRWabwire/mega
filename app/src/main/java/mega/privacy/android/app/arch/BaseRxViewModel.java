package mega.privacy.android.app.arch;

import androidx.lifecycle.AndroidViewModel;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import mega.privacy.android.app.MegaApplication;

public abstract class BaseRxViewModel extends AndroidViewModel {
    protected CompositeDisposable mCompositeDisposable = new CompositeDisposable();

    public BaseRxViewModel() {
        super(MegaApplication.getInstance());
    }

    protected void add(Disposable disposable) {
        mCompositeDisposable.add(disposable);
    }

    @Override protected void onCleared() {
        super.onCleared();

        mCompositeDisposable.clear();
    }
}
