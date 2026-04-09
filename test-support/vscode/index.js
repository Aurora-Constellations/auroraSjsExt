"use strict";

const createStub = () => {
  const target = function vscodeStub() {
    return stub;
  };

  const handler = {
    apply() {
      return stub;
    },
    construct() {
      return stub;
    },
    get(target, prop, receiver) {
      if (prop === "__esModule") return true;
      if (prop === "default") return stub;
      if (prop === "then") return undefined;
      if (prop === Symbol.toPrimitive) return () => "";
      if (prop === "toString") return () => "[vscode test stub]";
      if (prop === "valueOf") return () => 0;
      if (Reflect.has(target, prop)) return Reflect.get(target, prop, receiver);
      return stub;
    },
    getOwnPropertyDescriptor(target, prop) {
      const descriptor = Reflect.getOwnPropertyDescriptor(target, prop);
      if (descriptor !== undefined) return descriptor;
      if (prop === "__esModule") return { configurable: true, enumerable: false, value: true };
      if (prop === "default") return { configurable: true, enumerable: false, value: stub };
      return { configurable: true, enumerable: true, value: stub, writable: true };
    },
    has(target, prop) {
      return Reflect.has(target, prop) || prop === "__esModule" || prop === "default";
    },
    ownKeys(target) {
      return Array.from(new Set([...Reflect.ownKeys(target), "__esModule", "default"]));
    },
    set() {
      return true;
    }
  };

  const stub = new Proxy(target, handler);
  return stub;
};

const vscode = createStub();

module.exports = vscode;
module.exports.default = vscode;
module.exports.__esModule = true;
