package top.kzre.krro.util.arena;

public class AllocateResult<T extends ArenaView<T>> {
    private final T view;
    private final AllocateError error;
    private AllocateResult(T view, AllocateError error) {
        this.view = view;
        this.error = error;
    }

    public T getView() {
        return view;
    }

    public AllocateError getError() {
        return error;
    }

    public static <V extends ArenaView<V>> AllocateResult<V> success(V view) {
        return new AllocateResult<>(view, AllocateError.NO_ERROR);
    }

    public static <V extends ArenaView<V>> AllocateResult<V> failed(AllocateError error) {
        if (error == AllocateError.NO_ERROR) {
            throw new IllegalArgumentException("failed requires non-NO_ERROR");
        }
        return new AllocateResult<>(null, error);
    }

    public  static <V extends ArenaView<V>> AllocateResult<V > unknownFailed(){
        return new AllocateResult<V>(null, AllocateError.UNKNOWN);
    }

    public boolean isSuccess() {
        return error == AllocateError.NO_ERROR;
    }
    public boolean isFailure()  {
        return error != AllocateError.NO_ERROR;
    }
}
