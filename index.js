import React, { Component, PropTypes } from 'react';
import { requireNativeComponent, View } from 'react-native';

class OcrView extends Component {
  constructor(props) {
    super(props);
    this._onChange = this._onChange.bind(this);
  }

  static propTypes = {
    onChangeMessage: React.PropTypes.func
  };

  _onChange(event) {
    if (!this.props.onChangeMessage) {
      return;
    }
    this.props.onChangeMessage(event.nativeEvent.message);
  }

  render() {
    return <RCTOcrView {...this.props} onChange={this._onChange} />;
  }
};

export default requireNativeComponent(`RCTOcrView`, OcrView, {
  nativeOnly: {onChange: true}
});
