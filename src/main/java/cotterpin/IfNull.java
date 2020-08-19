package cotterpin;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class IfNull<P, T> {

	final BiConsumer<? super P, ? super T> record;
	final Supplier<? extends T> create;

	IfNull(BiConsumer<? super P, ? super T> record, Supplier<? extends T> create) {
		this.record = Objects.requireNonNull(record, "record");
		this.create = Objects.requireNonNull(create, "create");
	}
}
