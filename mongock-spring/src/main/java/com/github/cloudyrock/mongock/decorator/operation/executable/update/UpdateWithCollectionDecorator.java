package com.github.cloudyrock.mongock.decorator.operation.executable.update;

import com.github.cloudyrock.mongock.decorator.util.Invokable;
import com.github.cloudyrock.mongock.decorator.operation.executable.update.impl.UpdateWithQueryDecoratorImpl;
import org.springframework.data.mongodb.core.ExecutableUpdateOperation;

public interface UpdateWithCollectionDecorator<T> extends Invokable, ExecutableUpdateOperation.UpdateWithCollection<T> {

  ExecutableUpdateOperation.UpdateWithCollection<T> getImpl();

  @Override
  default ExecutableUpdateOperation.UpdateWithQuery<T> inCollection(String collection) {
    return new UpdateWithQueryDecoratorImpl<>(getInvoker().invoke(()-> getImpl().inCollection(collection)), getInvoker());
  }
}
